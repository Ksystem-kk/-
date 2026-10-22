# QRコード照合

Android端末のカメラでQRコードを2つ読み取り、データが完全一致するかを確認するアプリです。読み取れるデータは、`KKNI`で始まる全19文字または全20文字に限られます。1つ目のコードが条件に合わない場合は読み取りを続け、2つ目の条件に合うコードを読み取ると一致・不一致を表示します。

## ビルドと実行

- JDK 17とAndroid SDK Platform 35を用意し、Android Studioでこのリポジトリを開いてGradle同期後に実機またはカメラ付きエミュレーターで実行します。初回起動時にカメラ権限が必要です。
- コマンドラインでのビルド: `./gradlew :app:assembleDebug`
- 単体テスト: `./gradlew :app:testDebugUnitTest`
- 単一テストクラス: `./gradlew :app:testDebugUnitTest --tests 'com.example.qrcodematcher.QrCodeValidatorTest'`
- lint: `./gradlew :app:lintDebug`

## 構成

- `MainActivity`がCameraXのプレビューと画像解析を管理し、ML KitでQRコードだけを読み取ります。
- `QrCodeValidator`が読み取った生データの先頭文字と全体の文字数を判定します。条件を通過した値だけを1つ目として保持し、次の有効な値と完全一致で比較します。
- 1つ目を読み取った後、誤って同じQRを照合しないよう、カメラからQRが一度見えなくなってから2つ目を受け付けます。
- 読み取ったデータは端末内にも保存せず、比較結果だけを画面に表示します。
