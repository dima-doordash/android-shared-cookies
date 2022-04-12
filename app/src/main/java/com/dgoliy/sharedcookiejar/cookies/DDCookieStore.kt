package com.dgoliy.sharedcookiejar.cookies

import android.os.Build
import com.company.android.logging.DDLog
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

/**
 * In-memory Cookie Store.
 *
 * Kotlinized from
 * https://chromium.googlesource.com/android_tools/+/master/sdk/sources/android-25/java/net/InMemoryCookieStore.java
 */
class DDCookieStore(
    private val onCookieStoreError: ((Throwable) -> Unit)? = null
) : CookieStore {
    companion object {
        private const val TAG = "DDCookieStore"
    }

    // the in-memory representation of cookies
    private var uriIndex = HashMap<URI, MutableList<HttpCookie>>()

    // use ReentrantLock instead of synchronized for scalability
    private var lock = ReentrantLock(false)
    private val applyMCompatibility = Build.VERSION.SDK_INT <= 23

    private fun reportErrorOrThrow(t: Throwable) {
        onCookieStoreError?.invoke(t) ?: run {
            DDLog.e(
                TAG, "Failed to handle a CookieStore exception." +
                        "Provide onCookieStoreError for graceful error handling."
            )
            throw t
        }
    }

    /**
     * Add one cookie into cookie store.
     */
    override fun add(uri: URI?, cookie: HttpCookie?) {
        if (uri == null) {
            reportErrorOrThrow(NullPointerException("uri is null in `add`"))
            return
        }
        if (cookie == null) {
            reportErrorOrThrow(NullPointerException("cookie is null in `add`"))
            return
        }

        lock.lock()
        try {
            if (cookie.maxAge != 0L) {
                addIndex(getEffectiveURI(uri), cookie)
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Get all cookies, which:
     * 1) given uri domain-matches with, or, associated with
     * given uri when added to the cookie store.
     * 3) not expired.
     * See RFC 2965 sec. 3.3.4 for more detail.
     */
    override fun get(uri: URI?): List<HttpCookie> {
        // argument can't be null
        if (uri == null) {
            reportErrorOrThrow(NullPointerException("uri is null in `get`"))
            return emptyList()
        }
        val cookies: MutableList<HttpCookie> = ArrayList()
        lock.lock()
        try {
            // check domainIndex first
            getInternal1(cookies, uri.host)
            // check uriIndex then
            getInternal2(cookies, getEffectiveURI(uri))
        } finally {
            lock.unlock()
        }
        return cookies
    }

    /**
     * Get all cookies in cookie store, except those have expired
     */
    override fun getCookies(): List<HttpCookie> {
        var rt = mutableListOf<HttpCookie>()
        lock.lock()
        try {
            for (list in uriIndex.values) {
                val it = list.iterator()
                while (it.hasNext()) {
                    val cookie = it.next()
                    if (cookie.hasExpired()) {
                        it.remove()
                    } else if (!rt.contains(cookie)) {
                        rt.add(cookie)
                    }
                }
            }
        } finally {
            rt = Collections.unmodifiableList(rt)
            lock.unlock()
        }
        return rt
    }

    /**
     * Get all URIs, which are associated with at least one cookie
     * of this cookie store.
     */
    override fun getURIs(): List<URI> {
        val uris = mutableListOf<URI>()
        lock.lock()
        try {
            return Collections.unmodifiableList(uriIndex.keys.toList())
        } finally {
            uris.addAll(uriIndex.keys)
            lock.unlock()
        }
    }

    /**
     * Remove a cookie from store
     */
    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        // argument can't be null
        if (uri == null) {
            reportErrorOrThrow(NullPointerException("uri is null in `remove`"))
            return true
        }
        if (cookie == null) {
            reportErrorOrThrow(NullPointerException("cookie is null in `remove`"))
            return true
        }
        lock.lock()
        return try {
            val effectiveURI = getEffectiveURI(uri)
            if (uriIndex[effectiveURI] == null) {
                false
            } else {
                val cookies = uriIndex[effectiveURI]
                cookies?.remove(cookie) ?: false
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Remove all cookies in this cookie store.
     */
    override fun removeAll(): Boolean {
        lock.lock()
        val result: Boolean
        try {
            result = uriIndex.isNotEmpty()
            uriIndex.clear()
        } finally {
            lock.unlock()
        }
        return result
    }

    /* ---------------- Private operations -------------- */ /*
     * This is almost the same as HttpCookie.domainMatches except for
     * one difference: It won't reject cookies when the 'H' part of the
     * domain contains a dot ('.').
     * I.E.: RFC 2965 section 3.3.2 says that if host is x.y.domain.com
     * and the cookie domain is .domain.com, then it should be rejected.
     * However that's not how the real world works. Browsers don't reject and
     * some sites, like yahoo.com do actually expect these cookies to be
     * passed along.
     * And should be used for 'old' style cookies (aka Netscape type of cookies)
     */
    private fun netscapeDomainMatches(domain: String?, host: String?): Boolean {
        if (domain == null || host == null) {
            return false
        }
        // if there's no embedded dot in domain and domain is not .local
        val isLocalDomain = ".local".equals(domain, ignoreCase = true)
        var embeddedDotInDomain = domain.indexOf('.')
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1)
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length - 1)) {
            return false
        }
        // if the host name contains no dot and the domain name is .local
        val firstDotInHost = host.indexOf('.')
        if (firstDotInHost == -1 && isLocalDomain) {
            return true
        }
        val domainLength = domain.length
        val lengthDiff = host.length - domainLength
        return when {
            lengthDiff == 0 -> {
                // if the host name and the domain name are just string-compare euqal
                host.equals(domain, ignoreCase = true)
            }
            lengthDiff > 0 -> {
                // need to check H & D component
                val D = host.substring(lengthDiff)
                // Android M and earlier: Cookies with domain "foo.com" would not match "bar.foo.com".
                // The RFC dictates that the user agent must treat those domains as if they had a
                // leading period and must therefore match "bar.foo.com".
                if (applyMCompatibility && !domain.startsWith(".")) {
                    false
                } else {
                    D.equals(domain, ignoreCase = true)
                }
            }
            lengthDiff == -1 -> {
                // if domain is actually .host
                domain[0] == '.' && host.equals(domain.substring(1), ignoreCase = true)
            }
            else -> false
        }
    }

    private fun getInternal1(
        cookies: MutableList<HttpCookie>,
        host: String
    ) {
        // Use a separate list to handle cookies that need to be removed so
        // that there is no conflict with iterators.
        val toRemove = mutableListOf<HttpCookie>()
        for ((_, lst) in uriIndex) {
            for (c in lst) {
                val domain = c.domain
                if (c.version == 0 && netscapeDomainMatches(domain, host) ||
                    c.version == 1 && HttpCookie.domainMatches(domain, host)
                ) {
                    // the cookie still in main cookie store
                    if (!c.hasExpired()) {
                        // don't add twice
                        if (!cookies.contains(c)) {
                            cookies.add(c)
                        }
                    } else {
                        toRemove.add(c)
                    }
                }
            }
            // Clear up the cookies that need to be removed
            for (c in toRemove) {
                lst.remove(c)
            }
            toRemove.clear()
        }
    }

    // @param cookies           [OUT] contains the found cookies
    // @param cookieIndex       the index
    // @param comparator        the prediction to decide whether or not
    //                          a cookie in index should be returned
    private fun getInternal2(
        cookies: MutableList<HttpCookie>,
        comparator: URI
    ) {
        // Removed cookieJar
        for (index in uriIndex.keys) {
            if (index === comparator || comparator.compareTo(index) == 0) {
                val indexedCookies = uriIndex[index]
                // check the list of cookies associated with this domain
                if (indexedCookies != null) {
                    val it = indexedCookies.iterator()
                    while (it.hasNext()) {
                        val cookie = it.next()
                        // the cookie still in main cookie store
                        if (!cookie.hasExpired()) {
                            // don't add twice
                            if (!cookies.contains(cookie)) {
                                cookies.add(cookie)
                            }
                        } else {
                            it.remove()
                        }
                    }
                } // end of indexedCookies != null
            } // end of comparator.compareTo(index) == 0
        } // end of cookieIndex iteration
    }

    // add 'cookie' indexed by 'index' into 'indexStore'
    private fun addIndex(
        index: URI,
        cookie: HttpCookie
    ) {
        // Android-changed : "index" can be null. We only use the URI based
        // index on Android and we want to support null URIs. The underlying
        // store is a HashMap which will support null keys anyway.
        var cookies = uriIndex[index]
        if (cookies != null) {
            // there may already have the same cookie, so remove it first
            cookies.remove(cookie)
            cookies.add(cookie)
        } else {
            cookies = ArrayList()
            cookies.add(cookie)
            uriIndex[index] = cookies
        }
    }

    //
    // for cookie purpose, the effective uri should only be http://host
    // the path will be taken into account when path-match algorithm applied
    //
    private fun getEffectiveURI(uri: URI): URI {
        return try {
            URI(
                "http",
                uri.host,
                null,  // path component
                null,  // query component
                null // fragment component
            )
        } catch (ignored: URISyntaxException) {
            uri
        }
    }
}
