Android Emulator Proxy Fix Forwarder
=====================================

概要
----

Android Emulator のプロキシ実装不具合を回避するためのフォワーダです。

現行の Android Emulator のプロキシ実装には不具合があり、Proxy サーバの実装によっては
HTTPS 接続が失敗します (HTTP 接続は問題なし)。

具体的には、CONNECT メソッド発行時に、Proxy サーバのレスポンスにヘッダが1行でも含まれ
ていると接続が失敗します。

本フォワーダは本問題を解決するためのもので、Proxy サーバの前段にかませることで Proxy
サーバレスポンスのヘッダをスキップするものです。

使用方法
-------

Java 8 が必要です。
Android Emulator を動作させるホストと同じホストで実行してください。

    $ java -jar emufix-proxy-1.0.jar [local port] [upstream proxy server] [upstream proxy port]

local port に forwarder 側のポート番号、upstream proxy server, upstream proxy port に上位プロキシ
サーバのホスト名とポート番号を指定します。

Android Emulator の起動時には以下のオプションを指定してください。

    -http-proxy http://localhost:[local port]
