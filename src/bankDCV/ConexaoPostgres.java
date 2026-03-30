package bankDCV;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class ConexaoPostgres {

    public static Connection conectar() {
        try {
            Properties props = new Properties();
            props.load(ConexaoPostgres.class.getClassLoader()
                .getResourceAsStream("config.properties"));

            String url     = props.getProperty("db.url");
            String usuario = props.getProperty("db.usuario");
            String senha   = props.getProperty("db.senha");

            Connection conn = DriverManager.getConnection(url, usuario, senha);
            System.out.println("Conexão bem sucedida!");
            return conn;
        } catch (Exception e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
            return null;
        }
    }
}
