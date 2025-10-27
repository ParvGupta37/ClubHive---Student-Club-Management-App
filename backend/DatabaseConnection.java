import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:unihub.db";

    public static void main(String[] args) {
        try {
            // Load SQLite JDBC Driver
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                System.out.println("✅ Connected to SQLite database successfully!");

                Statement stmt = conn.createStatement();
                String[] tables = {"announcements", "members", "meetings", "clubs", "events", "notifications"};
                for (String t : tables)
                    stmt.execute("DROP TABLE IF EXISTS " + t);

                // --- Table Creation ---
                stmt.execute("""
                    CREATE TABLE clubs (
                        club_id TEXT PRIMARY KEY,
                        club_name TEXT,
                        mentor_name TEXT,
                        description TEXT,
                        created_at TIMESTAMP,
                        email TEXT,
                        password_hash TEXT
                    );
                """);

                stmt.execute("""
                    CREATE TABLE announcements (
                        announcement_id TEXT PRIMARY KEY,
                        club_id TEXT,
                        title TEXT,
                        message TEXT,
                        posted_at TIMESTAMP,
                        FOREIGN KEY (club_id) REFERENCES clubs(club_id)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE members (
                        member_id TEXT PRIMARY KEY,
                        club_id TEXT,
                        name TEXT,
                        roll_number TEXT,
                        email TEXT,
                        role TEXT,
                        joined_at TIMESTAMP,
                        FOREIGN KEY (club_id) REFERENCES clubs(club_id)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE meetings (
                        meeting_id TEXT PRIMARY KEY,
                        club_id TEXT,
                        title TEXT,
                        description TEXT,
                        meeting_date DATE,
                        meeting_time TIME,
                        created_at TIMESTAMP,
                        FOREIGN KEY (club_id) REFERENCES clubs(club_id)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE events (
                        event_id TEXT PRIMARY KEY,
                        club_id TEXT,
                        event_name TEXT,
                        date DATE,
                        venue TEXT,
                        description TEXT,
                        created_at TIMESTAMP,
                        FOREIGN KEY (club_id) REFERENCES clubs(club_id)
                    );
                """);

                stmt.execute("""
                    CREATE TABLE notifications (
                        notification_id TEXT PRIMARY KEY,
                        club_id TEXT,
                        message TEXT,
                        type TEXT,
                        created_at TIMESTAMP,
                        FOREIGN KEY (club_id) REFERENCES clubs(club_id)
                    );
                """);

                System.out.println("✅ Tables created successfully!");

                // ---------- Insert sample data ----------
                PreparedStatement psClub = conn.prepareStatement("""
                    INSERT INTO clubs VALUES (?, ?, ?, ?, ?, ?, ?)
                """);

                String[][] clubs = {
                    {"c1", "AI Club", "Dr. Meera Rao", "Explores AI, ML and data science through workshops and hackathons.", "aiclub@srm.edu"},
                    {"c2", "Robotics Club", "Prof. R. Narayanan", "Focuses on building and programming robots for competitions.", "robotics@srm.edu"},
                    {"c3", "Coding Ninjas", "Dr. Kavya Menon", "Club for coders passionate about problem solving and hackathons.", "codingninjas@srm.edu"},
                    {"c4", "CyberSec Society", "Mr. Arjun B.", "Spreads cybersecurity awareness through sessions and CTFs.", "cybersec@srm.edu"},
                    {"c5", "Eco Innovators", "Dr. Priya Sharma", "Works on sustainability projects and environmental awareness.", "ecoinnovators@srm.edu"},
                    {"c6", "Finance Club", "Prof. Shashank Rao", "Covers personal finance, stock markets, and entrepreneurship.", "finance@srm.edu"},
                    {"c7", "Music Circle", "Dr. Rekha Iyer", "Brings together music enthusiasts for jamming and performances.", "music@srm.edu"},
                    {"c8", "Design Hive", "Mr. Rohit T.", "A creative hub for designers exploring UI/UX and digital art.", "designhive@srm.edu"},
                    {"c9", "Literary League", "Dr. Ananya Das", "Club for writers, poets, and literature lovers.", "literary@srm.edu"},
                    {"c10", "Photon Pixels", "Prof. Manish Ghosh", "Photography and filmmaking club showcasing student creativity.", "photonpixels@srm.edu"}
                };

                for (String[] c : clubs) {
                    psClub.setString(1, c[0]);
                    psClub.setString(2, c[1]);
                    psClub.setString(3, c[2]);
                    psClub.setString(4, c[3]);
                    psClub.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    psClub.setString(6, c[4]);
                    psClub.setString(7, "hashed_" + c[0]);
                    psClub.executeUpdate();
                }

                PreparedStatement psMember = conn.prepareStatement("""
                    INSERT INTO members VALUES (?, ?, ?, ?, ?, ?, ?)
                """);

                String[][] members = {
                    {"m1", "c1", "Anna Sian", "RA2111003010001", "anna@srm.edu", "President"},
                    {"m2", "c1", "Shrish Roy", "RA2111003010002", "shrish@srm.edu", "Vice President"},
                    {"m3", "c2", "Nisha Agarwal", "RA2111003020001", "nisha@srm.edu", "President"},
                    {"m4", "c2", "Rohan Das", "RA2111003020002", "rohan@srm.edu", "Secretary"},
                    {"m5", "c3", "Aditya Menon", "RA2111003030001", "aditya@srm.edu", "President"},
                    {"m6", "c4", "Simran Kaur", "RA2111003040001", "simran@srm.edu", "Treasurer"},
                    {"m7", "c5", "Sahil Verma", "RA2111003050001", "sahil@srm.edu", "Coordinator"},
                    {"m8", "c6", "Aditi Rao", "RA2111003060001", "aditi@srm.edu", "Secretary"},
                    {"m9", "c7", "Karan Patel", "RA2111003070001", "karan@srm.edu", "President"},
                    {"m10", "c8", "Neha Sharma", "RA2111003080001", "neha@srm.edu", "Coordinator"}
                };

                for (String[] m : members) {
                    psMember.setString(1, m[0]);
                    psMember.setString(2, m[1]);
                    psMember.setString(3, m[2]);
                    psMember.setString(4, m[3]);
                    psMember.setString(5, m[4]);
                    psMember.setString(6, m[5]);
                    psMember.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now().minusDays(10)));
                    psMember.executeUpdate();
                }

                PreparedStatement psMeeting = conn.prepareStatement("""
                    INSERT INTO meetings VALUES (?, ?, ?, ?, ?, ?, ?)
                """);
                for (int i = 0; i < clubs.length; i++) {
                    psMeeting.setString(1, "mt" + (i + 1));
                    psMeeting.setString(2, clubs[i][0]);
                    psMeeting.setString(3, clubs[i][1] + " Weekly Meetup");
                    psMeeting.setString(4, "Discussion and planning session for " + clubs[i][1]);
                    psMeeting.setDate(5, Date.valueOf(LocalDate.now().plusDays(i + 1)));
                    psMeeting.setTime(6, Time.valueOf(LocalTime.now().plusHours(i + 1)));
                    psMeeting.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                    psMeeting.executeUpdate();
                }

                System.out.println("✅ Sample data inserted successfully!");
            }
        } catch (Exception e) {
            System.out.println("❌ Error initializing database!");
            e.printStackTrace();
        }
    }
}
