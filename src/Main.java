import com.fastcgi.FCGIInterface;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main {
    final static String fileName = "JustCGI.jar";
    final static String badresponse = "Запрос на сервер некорректен!";
    final static String successresponse = "Точка попала в область!";
    final static String loseresponse = "Точка не попала в область!";

    static FileWriter oup;
    public static boolean check_point(Double x, Double y, Double r)
    {
        if(x >= 0 && y <= 0)
        {
            return (x*x+y*y) <= r;
        }
        if(x <= 0 && y >= 0)
        {
            return (y<=r) && (x>=-r/2);
        }
        if(x <= 0 && y <= 0)
        {
            return -x-y<=r/2;
        }
        return false;
    }
    public static void main(String[] args) throws java.io.IOException {
        var time_start = System.nanoTime();
        oup = new FileWriter("log.txt");
        var fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0) {
            try {
                oup.append("Accepted\n");
                // Чтение тела запроса
                String requestBody = readRequestBody();
                //oup.append("This is body: " + requestBody + "\n");
                // Обработка данных (в данном случае просто выводим их)

                int ind = requestBody.indexOf(fileName+"?");
                String response = "";
                if(ind == -1)response = badresponse;
                else
                {
                    ind+=fileName.length()+1;
                    int end_ind = requestBody.indexOf("\"",ind);
                    if(end_ind == -1)response = badresponse;
                    else {
                        String[] params = requestBody.substring(ind, end_ind).split("&");
                        Double x = null,y = null,r = null;
                        for (var str : params) {
                            try
                            {
                                if (str.startsWith("x=")) {
                                    x = Double.parseDouble(str.substring(2));
                                } else if (str.startsWith("y=")) {
                                    y = Double.parseDouble(str.substring(2));
                                } else if (str.startsWith("r=")) {
                                    r = Double.parseDouble(str.substring(2));
                                    if(r < 0)
                                    {
                                        oup.append("R below zero: " + str + "\n");
                                        response = badresponse;
                                        break;
                                    }
                                } else {
                                    oup.append("Something else in response: " + str + "\n");
                                    response = badresponse;
                                    break;
                                }
                            }
                            catch (NumberFormatException ex)
                            {
                                oup.append("FormatExcept: " + ex.getMessage() + "\n");
                                response = badresponse;
                                break;
                            }
                        }
                        if(response.isEmpty())
                        {
                            if(x == null || y == null || r == null)
                            {
                                oup.append("Something is null\n");
                                response = badresponse;
                            }
                            else if(check_point(x,y,r))
                            {
                                oup.append("GoodShoot\n");
                                response = successresponse;
                            }
                            else
                            {
                                oup.append("Lose\n");
                                response = loseresponse;
                            }
                        }
                    }
                }
                oup.flush();

                // Формируем ответ
                String content = "<span>%s</span>".formatted(response);

                // Формирование HTTP-ответа
                String httpResponse = """
                        HTTP/1.1 200 OK
                        Content-Type: text/html
                        Content-Length: %d
                        Date: %s
                        Execution: %s
                    
                        %s
                        """.formatted(content.getBytes(StandardCharsets.UTF_8).length, LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), LocalTime.ofNanoOfDay(System.nanoTime()-time_start).format(DateTimeFormatter.ISO_TIME),content);

                System.out.println(httpResponse);

                // Отправка ответа обратно через FastCGI
                FCGIInterface.request.outStream.write(httpResponse.getBytes(StandardCharsets.UTF_8));

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String readRequestBody() throws IOException, InterruptedException {
        //FCGIInterface.request.inStream.fill();
        /*var ss = FCGIInterface.request.inStream.buffStop;
        oup.append("This is shit: "+ss+"\n");
        var contentLength = FCGIInterface.request.inStream.available();
        oup.append("This is content length: " + contentLength + "\n");
        var buffer = ByteBuffer.allocate(contentLength);
        var readBytes = FCGIInterface.request.inStream.read(buffer.array(), 0, contentLength);
        oup.append("Read length: " + contentLength + "\n");
        var requestBodyRaw = new byte[readBytes];
        buffer.get(requestBodyRaw);
        buffer.clear();
        return new String(requestBodyRaw, StandardCharsets.UTF_8);*/
        oup.append("Buff length: " + FCGIInterface.request.inStream.buff.length + "\n");
        return new String(FCGIInterface.request.inStream.buff, StandardCharsets.UTF_8);
    }
}