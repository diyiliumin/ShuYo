import 'package:shared_preferences/shared_preferences.dart';
import 'package:webview_flutter/webview_flutter.dart';

import '../../core/forum_url_resolver.dart';

class WebVpnSessionStore {
  WebVpnSessionStore({
    Future<SharedPreferences> Function()? preferencesLoader,
    WebViewCookieManager? cookieManager,
    Future<List<WebViewCookie>> Function(Uri domain)? cookieLoader,
    Future<void> Function(WebViewCookie cookie)? cookieSetter,
  })  : _preferencesLoader = preferencesLoader ?? SharedPreferences.getInstance,
        _cookieManager = cookieManager ??
            (cookieLoader == null || cookieSetter == null
                ? WebViewCookieManager()
                : null) {
    _cookieLoader =
        cookieLoader ?? (domain) => _cookieManager!.getCookies(domain: domain);
    _cookieSetter = cookieSetter ?? _cookieManager!.setCookie;
  }

  static const cachedCookiesKey = 'academic.auth.cached_cookies.webvpn';

  final Future<SharedPreferences> Function() _preferencesLoader;
  final WebViewCookieManager? _cookieManager;
  late final Future<List<WebViewCookie>> Function(Uri domain) _cookieLoader;
  late final Future<void> Function(WebViewCookie cookie) _cookieSetter;

  Future<void> clearCachedCookiesForReauthentication() async {
    await (await _preferencesLoader()).remove(cachedCookiesKey);
  }

  /// Removes a WebVPN session only after the gateway has explicitly rejected
  /// it. Transport failures must not call this method because a cached token
  /// may still be valid when connectivity returns.
  Future<void> clearInvalidSession() async {
    await clearCachedCookiesForReauthentication();

    final domains = <Uri>[
      Uri.parse(ForumUrlResolver.webVpnPortalUrl),
      Uri.parse('https://https-oauth-shu-edu-cn-443.webvpn.shu.edu.cn'),
      Uri.parse('https://https-newsso-shu-edu-cn-443.webvpn.shu.edu.cn'),
    ];
    for (final domain in domains) {
      final isPortalHost =
          domain.host == Uri.parse(ForumUrlResolver.webVpnPortalUrl).host;
      final isProxiedIdentityHost = domain.host.contains('oauth-shu-edu-cn') ||
          domain.host.contains('newsso-shu-edu-cn');
      List<WebViewCookie> cookies;
      try {
        cookies = await _cookieLoader(domain);
      } on Object {
        continue;
      }
      for (final cookie in cookies) {
        // Do not write an empty webvpn-token to proxy subdomains. Android can
        // retain it as a host-only shadow that overrides the next valid token.
        final belongsToWebVpnSession =
            (isPortalHost && cookie.name == 'webvpn-token') ||
                (isProxiedIdentityHost && cookie.name == 'SHU_OAUTH2');
        if (!belongsToWebVpnSession) continue;
        try {
          await _cookieSetter(
            WebViewCookie(
              name: cookie.name,
              value: '',
              domain: _normalizeCookieDomain(cookie.domain, domain.host),
              path: cookie.path.isEmpty ? '/' : cookie.path,
            ),
          );
        } on Object {
          // The persistent copy is already gone. Continue clearing the other
          // known gateway domains even if one WebView operation fails.
        }
      }
    }
  }

  String _normalizeCookieDomain(String value, String fallbackHost) {
    if (value.isEmpty) return fallbackHost;
    final parsed = Uri.tryParse(value);
    if (parsed != null && parsed.host.isNotEmpty) return parsed.host;
    final withoutScheme = value.replaceFirst(RegExp(r'^https?://'), '');
    final host = withoutScheme.split('/').first.split(':').first;
    return host.isEmpty ? fallbackHost : host;
  }
}
