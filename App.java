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
    // ★変更: Todoをサーバーが動いている間だけ覚えておく一覧です。
    static List<Todo> todos = new ArrayList<>();
    // ★変更: 次のTodoに付ける番号です。
    static int nextId = 1;

    // プログラムを開始する場所です。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるWebサーバーを用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // ★変更: 起動時に表示する1件目のサンプルTodoです。
        todos.add(new Todo(nextId++, "牛乳を買う"));
        // ★変更: 起動時に表示する2件目のサンプルTodoです。
        Todo egg = new Todo(nextId++, "卵を買う");
        egg.setDone(true);
        todos.add(egg);
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
                // ★変更: URL用の文字を日本語のTodo名に戻します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                // 追加: 空でないTodoだけを一覧に加えます。
                if (!title.isEmpty()) {
                    // ★変更: 番号とTodo名からTodoを1件作って追加します。
                    todos.add(new Todo(nextId, title));
                    nextId++;
                }
                // 追加: 一覧のあるトップページへ戻す場所を指定します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                // 追加: 入力フォームとHTMLのリストを作り始めます。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>";
                // ★変更: Todoを1件ずつ取り出します。
                for (Todo todo : todos) {
                    // ★変更: 終わったTodoだけに印を用意します。
                    String mark = "";
                    if (todo.isDone()) {
                        mark = " ✔";
                    }
                    // ★変更: Todo名と印をHTMLの項目にします。
                    html += "<li>" + todo.getTitle() + mark + "</li>";
                }
                // 追加: HTMLのリストを閉じます。
                html += "</ul>";
                // 追加: 作ったHTMLを返す文字にします。
                message = html;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } else {
                // ★変更: 見つからないページ用の返事です。
                message = "ページが見つかりません";
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

// ★変更: 1件のTodoの番号・内容・完了状態をまとめるクラスです。
class Todo {
    // ★変更: Todoの番号です。
    private final int id;
    // ★変更: Todoの内容です。
    private final String title;
    // ★変更: Todoが終わったかどうかです。
    private boolean done;

    // ★変更: 番号と内容を受け取り、最初は未完了にします。
    Todo(int id, String title) {
        this.id = id;
        this.title = title;
        this.done = false;
    }

    // ★変更: Todoの番号を読み出します。
    int getId() {
        return id;
    }

    // ★変更: Todoの内容を読み出します。
    String getTitle() {
        return title;
    }

    // ★変更: 完了しているかを読み出します。
    boolean isDone() {
        return done;
    }

    // ★変更: 完了状態を書き換えます。
    void setDone(boolean done) {
        this.done = done;
    }
}
