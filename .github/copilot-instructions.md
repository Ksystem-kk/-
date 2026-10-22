# Copilot instructions

## 応答言語

- ユーザーへの説明、要約、質問は日本語で返してください。コード、識別子、コマンド、引用した原文は元の表記を維持してください。

## リポジトリの構成と動作

- Kotlin製の単一モジュールAndroidアプリです。`MainActivity`がCameraXのプレビューと画像解析を管理し、ML KitのQR専用スキャナーでフレームを処理します。カメラ権限はActivity Result APIで要求します。
- `QrCodeValidator`はQRの生データが大文字の`KKNI`で始まり、全体で19文字または20文字であることを判定します。数字のみであることや、大文字小文字を無視することは追加要件がない限り仮定しないでください。
- `MainActivity`は有効な1つ目の値をメモリ上に保持し、スキャン結果を完全一致で比較します。2つ目を受け付ける前にQR未検出のフレームを一度確認し、同じコードを連続して読み取る誤比較を防ぎます。比較結果は画面に表示し、再試行で状態を初期化します。読み取った値を永続化したり正規化したりしないでください。
- 比較不一致は画面に赤字のエラーとして表示し、`ToneGenerator`で通知音を鳴らします。Activity終了時に音源を解放してください。
- Gradle設定はKotlin DSLです。Android SDKのバージョンや依存ライブラリの設定を変更するときは`app/build.gradle.kts`を確認してください。

## ビルド、テスト、lint

- JDK 17とAndroid SDK Platform 35が必要です。Gradle Wrapperを使用してください。
- デバッグビルド: `./gradlew :app:assembleDebug`
- 単体テスト全件: `./gradlew :app:testDebugUnitTest`
- テストクラス単位: `./gradlew :app:testDebugUnitTest --tests 'com.example.qrcodematcher.QrCodeValidatorTest'`
- テストメソッド単位: `./gradlew :app:testDebugUnitTest --tests 'com.example.qrcodematcher.QrCodeValidatorTest.acceptsValuesStartingWithKkniAtSupportedLengths'`
- Android lint: `./gradlew :app:lintDebug`

## テスト上の規約

- 現在の単体テストはJUnit 4を使用し、`app/src/test/java`に配置しています。QR形式の判定を変更する場合は`QrCodeValidatorTest`の受け入れ条件と拒否条件を合わせて更新してください。
