package jetty.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;

public class ImageUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 4848049705802230040L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		MultipartConfigElement multipartConfig = new MultipartConfigElement("/tmp");
		req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);

		resp.setContentType("text/plain");
		PrintWriter out = resp.getWriter();

		String user = req.getParameter("user");
		String password = req.getParameter("password");

		String[] arrName = req.getParameterValues("name");
		String[] arrEmail = req.getParameterValues("email");

		String robotName = req.getParameter("robotName");
		Part filePart = req.getPart("robotJarFile");
		String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

		InputStream fileContent = filePart.getInputStream();

		byte[] buffer = new byte[fileContent.available()];
		fileContent.read(buffer);

		// TODO save all in database
		// File targetFile = new File("/tmp/");
		// OutputStream outStream = new FileOutputStream(targetFile);
		// outStream.write(buffer);

		// for (Part part : req.getParts()) {

		// out.printf(req.getParameter("password"));

		// for (String header : part.getHeaderNames()) {
		// String headerName = part.getHeader(header) == null ? "" :
		// part.getHeader(header);
		// }

		// out.printf("Got part: name=%s, size=%d%n", part.getName(), part.getSize());
		// part.write(String.format("part-%02d.dat", i++));
		// }
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Set response content type
		resp.setContentType("text/html");

		// Actual logic goes here.
		PrintWriter out = resp.getWriter();
		out.println("<h1>sdsdsdsdsds</h1>");
	}
}
