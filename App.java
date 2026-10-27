// HTTPサーバーを使うための準備です。
import com.sun.net.httpserver.HttpServer;
// 追加: 読み書きで起きる入出力エラーを表すための準備です。
import java.io.IOException;
// ポート番号（通信の出入口番号）を指定するための準備です。
import java.net.InetSocketAddress;
// 追加: URLで送られた日本語を元の文字に戻すための準備です。
import java.net.URLDecoder;
// 追加: UTF-8（日本語を正しく扱う文字コード）を使うための準備です。
import java.nio.charset.StandardCharsets;
// 追加: todos.csv を読み書きするための準備です。
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // 追加: お買い物に使える予算です。
    static int budget = 5000;
    // 追加: Todoの保存先ファイルです。
    static final Path TODO_FILE = Paths.get("todos.csv");

    // プログラムを開始する場所です。
    public static void main(String[] args) throws Exception {
        // 追加: 起動時に保存済みのTodoと予算を読み込みます。
        load();
        // 8080番ポートで待ち受けるWebサーバーを用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
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
                // 変更: 新規登録では商品名だけを受け取ります。
                String value = body.substring(5);
                // ★変更: URL用の文字を日本語のTodo名に戻します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8);
                // 追加: 空でないTodoだけを一覧に加えます。
                if (!title.isEmpty()) {
                    // 変更: 新規登録時の金額は0円にします。
                    todos.add(new Todo(nextId, title, 0));
                    nextId++;
                }
                // 追加: 商品を追加したあとの一覧をファイルへ保存します。
                save();
                // 追加: 一覧のあるトップページへ戻す場所を指定します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/budget") && method.equals("POST")) {
                // 追加: フォームから送られた予算を受け取ります。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String value = body.startsWith("budget=") ? body.substring(7) : "";
                try {
                    int inputBudget = Integer.parseInt(URLDecoder.decode(value, StandardCharsets.UTF_8));
                    if (inputBudget >= 0) {
                        budget = inputBudget;
                    }
                } catch (NumberFormatException e) {
                }
                // 追加: 予算を変更したあとの内容をファイルへ保存します。
                save();
                // 追加: 予算を設定したあと、一覧のあるトップページへ戻ります。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/done") && method.equals("POST")) {
                // 変更: 商品ごとのフォームからidと金額を受け取ります。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String[] values = body.split("&price=", 2);
                if (values.length == 2 && values[0].startsWith("id=") && !values[1].isEmpty()) {
                    try {
                        int id = Integer.parseInt(values[0].substring(3));
                        int price = Integer.parseInt(URLDecoder.decode(values[1], StandardCharsets.UTF_8));
                        if (price >= 0) {
                            for (Todo todo : todos) {
                                if (todo.getId() == id) {
                                    // 変更: 入力した金額を保存して、購入済みにします。
                                    todo.setPrice(price);
                                    todo.setDone(true);
                                    break;
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                    }
                }
                // 追加: 購入状態と金額を変更したあとの一覧をファイルへ保存します。
                save();
                // ★追加: 一覧のあるトップページへ戻します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/undone") && method.equals("GET")) {
                // 追加: URLのidを受け取り、購入済みを未購入へ戻します。
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=") && query.length() > 3) {
                    try {
                        int id = Integer.parseInt(query.substring(3));
                        for (Todo todo : todos) {
                            if (todo.getId() == id) {
                                // 追加: 見つけた商品を未購入に戻します。
                                todo.setDone(false);
                                break;
                            }
                        }
                    } catch (NumberFormatException e) {
                    }
                }
                // 追加: 購入を取り消したあとの一覧をファイルへ保存します。
                save();
                // 追加: 一覧のあるトップページへ戻します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/delete") && method.equals("GET")) {
                // ★追加: URLのidを受け取ります。
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=") && query.length() > 3) {
                    try {
                        // ★追加: idを数字に変えて、同じ番号のTodoを削除します。
                        int id = Integer.parseInt(query.substring(3));
                        todos.removeIf(todo -> todo.getId() == id);
                    } catch (NumberFormatException e) {
                    }
                }
                // 追加: 商品を削除したあとの一覧をファイルへ保存します。
                save();
                // ★追加: 一覧のあるトップページへ戻します。
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/")) {
                // 追加: 購入済みの商品だけを合計して、予算の残りを計算します。
                int spent = 0;
                for (Todo todo : todos) {
                    if (todo.isDone()) {
                        spent += todo.getPrice();
                    }
                }
                int remaining = budget - spent;
                String budgetStatus = remaining < 0 ? "over" : "";
                String budgetMessage = remaining < 0 ? "予算を " + (-remaining) + "円 超えています" : "あと " + remaining + "円 使えます";
                // 見た目変更: ミント色のカード型レイアウト用のCSSをHTML内に書きます。
                String html = "<!doctype html><html><head><meta charset='UTF-8'><style>"
                        + "*{box-sizing:border-box;}body{margin:0;background:#eaf8f2;color:#24584a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Hiragino Kaku Gothic ProN',sans-serif;}"
                        + ".page{width:100%;max-width:700px;margin:0 auto;padding:32px 18px;}.card{background:#fff;border-radius:22px;padding:30px;box-shadow:0 10px 28px rgba(57,125,101,.14);}"
                        + "h1{margin:0 0 24px;color:#19735b;font-size:30px;text-align:center;}h2{margin:0;padding-bottom:10px;border-bottom:2px solid #cbeee0;color:#2c8067;font-size:21px;}"
                        + ".budget-panel{margin:-4px 0 26px;padding:18px;border-radius:16px;background:#f1fbf6;border:1px solid #d4eee2;}.budget-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;}.budget-value{padding:10px;border-radius:12px;background:#fff;text-align:center;}.budget-value span{display:block;color:#5b8879;font-size:13px;}.budget-value strong{color:#23795f;font-size:19px;}.budget-value.over strong,.budget-message.over{color:#bd4a45;}.budget-message{margin:12px 0 10px;text-align:center;font-weight:bold;color:#2a8065;}.budget-form{display:flex;align-items:center;gap:8px;}.budget-form label{font-size:14px;font-weight:bold;}.budget-form input{min-width:0;flex:1;padding:9px 10px;border:1px solid #abdcca;border-radius:10px;font-size:15px;}.budget-form button{border:0;border-radius:10px;padding:9px 14px;background:#6bbd9f;color:#fff;font-weight:bold;cursor:pointer;}"
                        + ".add-form{display:flex;gap:10px;margin-bottom:28px;}.add-form input{min-width:0;flex:1;padding:12px 14px;border:1px solid #abdcca;border-radius:12px;font-size:16px;outline:none;}.add-form input:focus,.buy-form input:focus{border-color:#45a987;box-shadow:0 0 0 3px #def4eb;}"
                        + ".add-form button{border:0;border-radius:12px;padding:12px 20px;background:#3baf89;color:#fff;font-size:16px;font-weight:bold;cursor:pointer;}.list-section{margin-top:26px;}"
                        + ".todo-list{margin:14px 0 0;padding:0;list-style:none;}.todo-item{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 0;border-bottom:1px solid #e4f3ed;line-height:1.6;}.todo-title{font-size:17px;}.price{margin-left:7px;color:#4f8976;font-size:14px;font-weight:bold;}.actions{display:flex;align-items:center;flex-wrap:wrap;gap:7px;}.buy-form{display:flex;align-items:center;gap:7px;}.buy-form input{width:105px;padding:6px 8px;border:1px solid #abdcca;border-radius:999px;font-size:14px;}.buy-form button{border:0;cursor:pointer;}"
                        + ".action{display:inline-block;padding:6px 10px;border-radius:999px;text-decoration:none;font-size:14px;font-weight:bold;}.buy{background:#d9f3e8;color:#19735b;}.undo{background:#e8f5d9;color:#527d24;}.delete{background:#fde8e7;color:#ae4d49;}"
                        + "@media(max-width:480px){.page{padding:16px 12px;}.card{padding:22px 18px;}h1{font-size:26px;}.budget-stats{grid-template-columns:1fr;}.budget-form{flex-wrap:wrap;}.budget-form input{min-width:120px;}.todo-item{align-items:flex-start;flex-direction:column;gap:7px;}.actions{width:100%;}.add-form{flex-wrap:wrap;}.add-form button{padding:12px 16px;}}</style></head><body><div class='page'><main class='card'><h1>🛒 今日のお買い物</h1>"
                        + "<section class='budget-panel'><div class='budget-stats'><div class='budget-value'><span>予算</span><strong>" + budget + "円</strong></div><div class='budget-value'><span>使用金額</span><strong>" + spent + "円</strong></div><div class='budget-value " + budgetStatus + "'><span>残り予算</span><strong>" + remaining + "円</strong></div></div><p class='budget-message " + budgetStatus + "'>" + budgetMessage + "</p><form class='budget-form' method='post' action='/budget'><label>予算（円）</label><input name='budget' type='number' min='0' value='" + budget + "'><button>設定</button></form></section>"
                        + "<form class='add-form' method='post' action='/add'><input name='todo' placeholder='買うものを入力' required><button>追加</button></form>";
                // 見た目変更: 未購入の商品を上側の見やすい一覧にします。
                html += "<section class='list-section'><h2>🧺 これから買うもの</h2><ul class='todo-list'>";
                for (Todo todo : todos) {
                    if (!todo.isDone()) {
                        // 変更: 未購入の商品ごとに、金額を入れて購入するフォームを表示します。
                        html += "<li class='todo-item'><span class='todo-title'>" + todo.getTitle() + "</span><div class='actions'><form class='buy-form' method='post' action='/done'><input type='hidden' name='id' value='" + todo.getId() + "'><input name='price' type='number' min='0' value='" + todo.getPrice() + "' placeholder='金額（円）' required><button class='action buy'>買った！</button></form><a class='action delete' href='/delete?id=" + todo.getId() + "'>削除</a></div></li>";
                    }
                }
                // 見た目変更: 購入済みの商品を下側の見やすい一覧にします。
                html += "</ul></section><section class='list-section'><h2>✅ 買ったもの</h2><ul class='todo-list'>";
                for (Todo todo : todos) {
                    if (todo.isDone()) {
                        // 見た目変更: 機能とURLはそのまま、取り消しと削除を押しやすく表示します。
                        html += "<li class='todo-item'><span class='todo-title'>" + todo.getTitle() + "<span class='price'>" + todo.getPrice() + "円</span></span><span class='actions'><a class='action undo' href='/undone?id=" + todo.getId() + "'>買ってない！</a><a class='action delete' href='/delete?id=" + todo.getId() + "'>削除</a></span></li>";
                    }
                }
                // 見た目変更: カードとHTMLを閉じます。
                html += "</ul></section></main></div></body></html>";
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

// 追加: 現在の予算とTodo一覧をUTF-8のtodos.csvへ保存します。
static void save() throws IOException {
    // 追加: ファイルへ書き出す行を用意します。
    List<String> lines = new ArrayList<>();
    // 追加: 予算は先頭行へ保存します。
    lines.add("budget," + budget);
    // 追加: Todoを1件ずつCSVの行にします。
    for (Todo todo : todos) {
        lines.add(todo.getId() + "," + (todo.isDone() ? "1" : "0") + "," + todo.getTitle() + "," + todo.getPrice());
    }
    // 追加: すべての行をUTF-8で保存します。
    Files.write(TODO_FILE, lines, StandardCharsets.UTF_8);
}

// 追加: todos.csvがあれば、予算とTodo一覧を起動時に読み込みます。
static void load() throws IOException {
    // 追加: 保存ファイルがなければ空の一覧のまま起動します。
    if (!Files.exists(TODO_FILE)) {
        return;
    }
    // 追加: UTF-8で保存された行を1行ずつ読み込みます。
    for (String line : Files.readAllLines(TODO_FILE, StandardCharsets.UTF_8)) {
        if (line.startsWith("budget,")) {
            // 追加: 先頭行の予算を復元します。
            budget = Integer.parseInt(line.substring(7));
        } else {
            // 追加: Todoのid・購入状態・商品名・金額を取り出します。
            int firstComma = line.indexOf(',');
            int secondComma = line.indexOf(',', firstComma + 1);
            int lastComma = line.lastIndexOf(',');
            int id = Integer.parseInt(line.substring(0, firstComma));
            boolean done = line.substring(firstComma + 1, secondComma).equals("1");
            String title = lastComma > secondComma ? line.substring(secondComma + 1, lastComma) : line.substring(secondComma + 1);
            int price = lastComma > secondComma ? Integer.parseInt(line.substring(lastComma + 1)) : 0;
            Todo todo = new Todo(id, title, price);
            todo.setDone(done);
            todos.add(todo);
            // 追加: 読み込んだ最大idの次を、新しい商品の番号にします。
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
    }
}

}

// ★変更: 1件のTodoの番号・内容・完了状態をまとめるクラスです。
class Todo {
    // ★変更: Todoの番号です。
    private final int id;
    // ★変更: Todoの内容です。
    private final String title;
    // 追加: Todoの金額です。
    private int price;
    // ★変更: Todoが終わったかどうかです。
    private boolean done;

    // 変更: 番号・内容・金額を受け取り、最初は未完了にします。
    Todo(int id, String title, int price) {
        this.id = id;
        this.title = title;
        this.price = price;
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

    // 追加: Todoの金額を読み出します。
    int getPrice() {
        return price;
    }

    // 追加: 購入時に入力した金額を書き換えます。
    void setPrice(int price) {
        this.price = price;
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
