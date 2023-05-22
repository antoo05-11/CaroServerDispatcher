import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SQLConnection sqlConnection = new SQLConnection();
        InputStream fstream = Main.class.getResourceAsStream("config.txt");
        sqlConnection.configure(fstream, "sql-server");
        sqlConnection.connectServer();

        Thread checkConnection = new Thread() {
            @Override
            public void run() {
                super.run();
                Hashtable<Integer, String> checkList = new Hashtable<>();
                while (true) {
                    String query = "select * from temporary_users where status != 'offline'";
                    ResultSet resultSet = sqlConnection.getDataQuery(query);
                    try {
                        while (resultSet.next()) {
                            if (checkList.containsKey(resultSet.getInt("userID"))) {
                                if (!resultSet.getString("connectionMessage").contains(checkList.get(resultSet.getInt("userID"))+"OK")) {
                                    query = "update temporary_users set status = 'offline' where userID = " + resultSet.getInt("userID");
                                    checkList.put(resultSet.getInt("userID"), "killed");
                                    sqlConnection.updateQuery(query);
                                }
                            }
                            String matchHashCode = LocalDateTime.now().toString() + resultSet.getInt("userID") + Math.random() * 10000;
                            checkList.put(resultSet.getInt("userID"), matchHashCode);
                            query = String.format("update temporary_users set connectionMessage = '%s' where userID = %d;", matchHashCode, resultSet.getInt("userID"));
                            sqlConnection.updateQuery(query);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        checkConnection.start();

        while (true) {
            System.out.println(sqlConnection.getConnection());
            String query = "select * from temporary_users where status = 'find_opponent';";
            ResultSet resultSet = sqlConnection.getDataQuery(query);
            List<Integer> userID = new ArrayList<>();
            List<Boolean> solvedID = new ArrayList<>();
            try {
                while (resultSet.next()) {
                    System.out.println(resultSet.getInt("userID"));
                    userID.add(resultSet.getInt("userID"));
                    solvedID.add(false);
                }
                for (int i = 0; i < userID.size() / 2; i++) {
                    System.out.println("break1");
                    int userID1 = -1;
                    int index = -1;
                    while (index == -1 || solvedID.get(index)) {
                        index = (int) (Math.random() * userID.size());
                        System.out.println("index: " + index);
                        userID1 = userID.get(index);
                    }
                    solvedID.set(index, true);
                    System.out.println("break2");
                    int userID2 = -1;
                    index = -1;
                    while (index == -1 || solvedID.get(index) || userID2 == userID1) {
                        index = (int) (Math.random() * userID.size());
                        userID2 = userID.get(index);
                    }
                    solvedID.set(index, true);
                    int firstTurnID = (Math.random() > 0.5) ? userID1 : userID2;
                    System.out.println(userID1 + " " + userID2 + " " + firstTurnID);

                    String matchHashCode = LocalDateTime.now().toString() + userID1 + userID2 + firstTurnID + Math.random() * 10000;

                    query = String.format("insert into matches (userID1, userID2, userTurnID, firstTurnID, matchHashCode) values (%d, %d, %d, %d, '%s')", userID1, userID2, firstTurnID, firstTurnID, matchHashCode);
                    sqlConnection.updateQuery(query);

                    query = String.format("select matchID from matches where matchHashCode = '%s';", matchHashCode);
                    int matchID = 0;
                    resultSet = sqlConnection.getDataQuery(query);
                    if (resultSet.next()) {
                        matchID = resultSet.getInt("matchID");
                    }

                    query = String.format("update temporary_users set status = 'in_game', matchID = %d where userID = %d or userID = %d", matchID, userID1, userID2);
                    sqlConnection.updateQuery(query);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            Thread.sleep(2000);
        }
    }
}

