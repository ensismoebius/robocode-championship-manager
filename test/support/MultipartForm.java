package support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A form of the browser sent as multipart/form-data: the fields and the files of the robot form. */
public final class MultipartForm {

	private final String boundary = "----" + UUID.randomUUID();
	private final List<byte[]> parts = new ArrayList<byte[]>();

	public MultipartForm field(String name, String value) {
		this.parts.add(("--" + this.boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
		return this;
	}

	public MultipartForm file(String name, String fileName, byte[] content) {
		this.parts.add(("--" + this.boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName
				+ "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		this.parts.add(content);
		this.parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
		return this;
	}

	/** A client that keeps the cookies of the session, for the forms that log the team in. */
	public static HttpClient session() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
	}

	/** Posts the form to the url with the client and returns the page that answers, following the redirects. */
	public String post(HttpClient client, String url) throws IOException, InterruptedException {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		for (byte[] part : this.parts) {
			body.writeBytes(part);
		}
		body.writeBytes(("--" + this.boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "multipart/form-data; boundary=" + this.boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
		return follow(client, client.send(request, HttpResponse.BodyHandlers.ofString()));
	}

	/** The page at the end of the redirects of a response. */
	public static String follow(HttpClient client, HttpResponse<String> response) throws IOException, InterruptedException {
		HttpResponse<String> current = response;
		while (current.statusCode() / 100 == 3) {
			URI location = current.request().uri().resolve(current.headers().firstValue("Location").orElseThrow());
			current = client.send(HttpRequest.newBuilder(location).build(), HttpResponse.BodyHandlers.ofString());
		}
		return current.body();
	}
}
