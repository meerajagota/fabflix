import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.gson.JsonParser;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String id = request.getParameter("id");

        // The log message can be found in localhost log
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            // Get a connection from dataSource

            // Construct a query with parameter represented by "?"
            String query = "SELECT " +
                    "    s.id AS starId, " +
                    "    s.name AS star_name, " +
                    "    s.birthYear AS star_dob, " +
                    "    m.id AS movieId, " +
                    "    m.title AS movie_title " +
                    "FROM " +
                    "    stars AS s " +
                    "JOIN " +
                    "    stars_in_movies AS sim ON s.id = sim.starId " +
                    "JOIN " +
                    "    movies AS m ON m.id = sim.movieId " +
                    "WHERE " +
                    "    s.id = ?";

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Set the parameter represented by "?" in the query to the id we get from url,
            // num 1 indicates the first "?" in the query
            statement.setString(1, id);

            // Perform the query
            ResultSet rs = statement.executeQuery();

            JsonObject jsonObject = new JsonObject();

            // Iterate through each row of rs
            while (rs.next()) {

                String starId = rs.getString("starId");
                String starName = rs.getString("star_name");
                String starDob = rs.getString("star_dob");
                String movieId = rs.getString("movieId");
                String movieTitle = rs.getString("movie_title");

                if (!jsonObject.has("star_id")) {
                    jsonObject.addProperty("star_id", starId);
                    jsonObject.addProperty("star_name", starName);
                    jsonObject.addProperty("star_dob", starDob);

                    JsonArray moviesArray = new JsonArray();
                    JsonObject movieObject = new JsonObject();
                    movieObject.addProperty("movie_id", movieId); // Add movieId
                    movieObject.addProperty("movie_title", movieTitle);

                    moviesArray.add(movieObject);

                    // Add the movies array to the JSON object
                    jsonObject.add("movies", moviesArray);
                } else {
                    // If the star details already exist, just add new movies
                    JsonArray moviesArray = jsonObject.getAsJsonArray("movies");

                    JsonObject movieObject = new JsonObject();
                    movieObject.addProperty("movie_id", movieId);
                    movieObject.addProperty("movie_title", movieTitle);

                    moviesArray.add(movieObject);
                }
            }
            rs.close();
            statement.close();

            //jsonObject.add("movies", moviesArray);
            // Write JSON string to output
            out.write(jsonObject.toString());
            // Set response status to 200 (OK)
            response.setStatus(200);

        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }

}