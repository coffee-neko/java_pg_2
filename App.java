// HTTPサーバーを使うための準備です。
import com.sun.net.httpserver.HttpServer;
// ポート番号（通信の出入口番号）を指定するための準備です。
import java.net.InetSocketAddress;
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
        // 「/」へのアクセスに返事をする処理を直接書きます。
        server.createContext("/", exchange -> {
            // ブラウザへ返す文字です。
            String path = exchange.getRequestURI().getPath();
            String message;
            // 文字の形式をUTF-8に指定して、日本語の文字化けを防ぎます。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            if (path.equals("/")) {
                message = "Hello, Server!";
            } else if (path.equals("/hello")) {
                // 追加: URLのクエリ（?name=Taro の部分）を受け取ります。
                String query = exchange.getRequestURI().getRawQuery();
                // 追加: name= の後ろを名前として切り出します。
                String name = query.substring(5);
                // 追加: 切り出した名前を返す文字に入れます。
                message = "こんにちは、" + name + "さん！";
            } else if (path.equals("/todos")) {
                // 追加: Todoの一覧を入れるListを作ります。
                List<String> todos = new ArrayList<>();
                // 追加: 1件目のTodoを入れます。
                todos.add("牛乳を買う");
                // 追加: 2件目のTodoを入れます。
                todos.add("卵を買う");
                // 追加: 3件目のTodoを入れます。
                todos.add("パンを買う");
                // 追加: HTMLのリストを作り始めます。
                String html = "<ul>";
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
