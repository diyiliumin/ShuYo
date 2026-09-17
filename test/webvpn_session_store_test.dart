import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shuyo/data/services/webvpn_session_store.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() {
  setUp(() => SharedPreferences.setMockInitialValues({}));

  test('clears a rejected WebVPN session without touching forum cookies',
      () async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      WebVpnSessionStore.cachedCookiesKey,
      '{"portal":[{"name":"webvpn-token","value":"stale","domain":"webvpn.shu.edu.cn","path":"/"}]}',
    );
    final cleared = <WebViewCookie>[];
    final store = WebVpnSessionStore(
      cookieLoader: (domain) async => [
        WebViewCookie(
          name: 'webvpn-token',
          value: 'stale',
          domain: domain.host,
        ),
        WebViewCookie(
          name: '_forum_session',
          value: 'forum-session',
          domain: domain.host,
        ),
        WebViewCookie(
          name: 'SHU_OAUTH2',
          value: 'oauth-session',
          domain: domain.host,
        ),
      ],
      cookieSetter: (cookie) async => cleared.add(cookie),
    );

    await store.clearInvalidSession();

    expect(prefs.getString(WebVpnSessionStore.cachedCookiesKey), isNull);
    expect(
      cleared.where((cookie) => cookie.name == 'webvpn-token'),
      isNotEmpty,
    );
    expect(cleared.any((cookie) => cookie.name == '_forum_session'), isFalse);
    expect(
      cleared.where((cookie) => cookie.name == 'SHU_OAUTH2').every(
            (cookie) =>
                cookie.domain.contains('oauth-shu-edu-cn') ||
                cookie.domain.contains('newsso-shu-edu-cn'),
          ),
      isTrue,
    );
  });
}
