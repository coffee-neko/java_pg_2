// HTTPサーバーを使うための準備です。
import com.sun.net.httpserver.HttpServer;
// ポート番号（通信の出入口番号）を指定するための準備です。
import java.net.InetSocketAddress;
// 追加: URLで送られた日本語を元の文字に戻すための準備です。
import java.net.URLDecoder;
// 追加: UTF-8（日本語を正しく扱う文字コード）を使うための準備です。
import java.nio.charset.StandardCharsets;
// 追加: Todoの一覧を扱うListを使うための準備です。
import java.util.ArrayList;
// 追加: Todoの一覧を扱うListを使うための準備です。
import java.util.List;

// プログラム本体の名前を App にします。
public class App {
    // プログラムを開始する場所です。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるWebサーバーを用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // 追加: Todoをサーバーが動いている間だけ覚えておく一覧です。
        List<String> todos = new ArrayList<>();
        // 「/」へのアクセスに返事をする処理を直接書きます。
        server.createContext("/", exchange -> {
            // ブラウザへ返す文字です。
            String path = exchange.getRequestURI().getPath();
            // 追加: GETかPOSTかという送信方法を受け取ります。
            String method = exchange.getRequestMethod();
            String message;
            // 文字の形式をUTF-8に指定して、日本語の文字化けを防ぎます。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            if (path.equals("/add") && method.equals("POST")) {
                // 追加: フォームから送られた内容を受け取ります。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // 追加: todo= の後ろの文字を切り出します。
                String value = body.substring(5);
                // 追加: URL用の文字を日本語に戻します。
                String todo = URLDecoder.decode(value, StandardCharsets.UTF_8);
                // 追加: 空でないTodoだけを一覧に加えます。
                if (!todo.isEmpty()) {
                    todos.add(todo);
                }
                // 追加: 一覧のあるトップページへ戻す場所を指定します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                // 追加: 入力フォームとHTMLのリストを作り始めます。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>";
                // 追加: Todoを1件ずつ取り出します。
                for (String todo : todos) {
                    // 追加: TodoをHTMLの項目にします。
                    html += "<li>" + todo + "</li>";
                }
                // 追加: HTMLのリストを閉じます。
                html += "</ul>";
                // 追加: 作ったHTMLを返す文字にします。
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else if (path.equals("/hello")) {
                // 追加: URLのクエリ（?name=Taro の部分）を受け取ります。
                String query = exchange.getRequestURI().getRawQuery();
                // 追加: name= の後ろを名前として切り出します。
                String name = query.substring(5);
                // 追加: 切り出した名前を返す文字に入れます。
                message = "こんにちは、" + name + "さん！";
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else {
                message = "Not Found";
            }
            // 返す文字を送信できるデータに変換します。
            byte[] body = message.getBytes("UTF-8");
            // 成功（200）とデータの長さをブラウザへ伝えます。
            exchange.sendResponseHeaders(200, body.length);
            // データをブラウザへ送ります。
            exchange.getResponseBody().write(body);
            // 通信を閉じます。
            exchange.getResponseBody().close();
        });
        // サーバーを起動します。
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
