Android Emulator Proxy Fix Forwarder
=====================================

[![Build Status](https://travis-ci.org/tmurakam/android-emulator-proxy-forwarder.svg?branch=master)](https://travis-ci.org/tmurakam/android-emulator-proxy-forwarder)

概要
----

Android Emulator のプロキシ実装不具合を回避するためのフォワーダです。

現行の Android Emulator のプロキシ実装には不具合( [Issue 72551](https://code.google.com/p/android/issues/detail?id=75221) )があり、Proxy サーバの実装によってはHTTPS 接続が失敗します (HTTP 接続は問題なし)。

具体的には、CONNECT メソッド発行時に、Proxy サーバのレスポンスにヘッダが1行でも含まれていると接続が失敗します。

本フォワーダは本問題を解決するためのもので、Proxy サーバの前段に配置することで Proxy サーバレスポンスを書き換えます。

必要環境
--------

JDK 8 が必要です。

ビルド手順
----------

    $ ./gradlew build

build/libs/emufix-proxy-1.0.jar が生成されます。

使用方法
--------

Android Emulator を動作させるホストと同じホストで実行してください。

    $ java -jar emufix-proxy-1.0.jar [local port] [upstream proxy server] [upstream proxy port]

local port に forwarder 側のポート番号、upstream proxy server, upstream proxy port に上位プロキシ
サーバのホスト名とポート番号を指定します。

Android Emulator の起動時には以下のオプションを指定してください。

    -http-proxy http://localhost:[local port]
