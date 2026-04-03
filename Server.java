import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class Server {

    // إعدادات المنفذ (Port) واسم ملف قاعدة البيانات
    private static final int PORT = 8000;
    private static final String DB_FILE = "database.json";

    public static void main(String[] args) throws Exception {
        // 1. إنشاء السيرفر على المنفذ 8000
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // 2. تحديد المسارات (Endpoints)
        // طلب الحصول على البيانات (GET)
        server.createContext("/api/data", new DataHandler());
        
        // 3. تشغيل السيرفر
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("------------------------------------------------");
        System.out.println("✅ تم تشغيل السيرفر بنجاح!");
        System.out.println("🌐 الرابط: http://localhost:" + PORT);
        System.out.println("📁 ملف قاعدة البيانات: " + new File(DB_FILE).getAbsolutePath());
        System.out.println("------------------------------------------------");
    }

    // هذا الكلاس يعالج الطلبات (GET و PUT)
    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // السماح بالاتصال من أي مصدر (CORS) ليتمكن ملف HTML من الاتصال
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Master-Key");

            // التعامل مع طلبات التحقق المسبق (Preflight OPTIONS)
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String method = exchange.getRequestMethod();
            String response = "";

            // --- طلب قراءة البيانات (GET) ---
            if ("GET".equals(method)) {
                try {
                    String data = readDatabase();
                    // نغلف البيانات بـ { "record": ... } لتناسب كود HTML القديم
                    response = "{ \"record\": " + data + " }";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } catch (Exception e) {
                    response = "{ \"error\": \"Error reading DB\" }";
                    exchange.sendResponseHeaders(500, response.getBytes().length);
                }
            } 
            // --- طلب حفظ البيانات (PUT) ---
            else if ("PUT".equals(method)) {
                try {
                    // قراءة البيانات القادمة من ملف HTML
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    writeDatabase(body);
                    
                    response = "{ \"message\": \"Saved successfully\" }";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "{ \"error\": \"Error saving DB\" }";
                    exchange.sendResponseHeaders(500, response.getBytes().length);
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }

            // إرسال الرد
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // دالة لقراءة ملف قاعدة البيانات
    private static String readDatabase() throws IOException {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            // إذا لم يكن الملف موجوداً، أنشئ مصفوفة فارغة
            return "[]";
        }
        return new String(Files.readAllBytes(Paths.get(DB_FILE)));
    }

    // دالة للكتابة في ملف قاعدة البيانات
    private static void.writeDatabase(String json) throws IOException {
        try (FileWriter writer = new FileWriter(DB_FILE)) {
            writer.write(json);
        }
    }
}
