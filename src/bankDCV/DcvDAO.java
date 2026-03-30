package bankDCV;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class DcvDAO {
    private Connection conn;

    public DcvDAO(Connection conn) {
        this.conn = conn;
    }

    public void inserirCliente(Bank_DCV.WsCliente cliente) {
        String sql = "INSERT INTO cliente (cpf, nome, agencia, conta, digito, tipo_conta) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, cliente.wsCpf);
            ps.setString(2, cliente.wsNome);
            ps.setString(3, cliente.wsAgencia);
            ps.setString(4, cliente.wsConta);
            ps.setInt(5, cliente.wsDigito);
            ps.setString(6, cliente.wsTipoConta.name());
            ps.executeUpdate();
            System.out.println("Cliente inserido com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao inserir cliente: " + e.getMessage());
        }
    }

    public void inserirFinanceiro(Bank_DCV.WsFinanceiro financeiro, String cpf) {
        String sql = "INSERT INTO financeiro (cpf, saldo_atual, limite_cheque, saldo_total, limite_pix) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, cpf);
            ps.setDouble(2, financeiro.wsSaldoAtual);
            ps.setDouble(3, financeiro.wsLimiteCheque);
            ps.setDouble(4, financeiro.wsSaldoTotal);
            ps.setDouble(5, financeiro.wsLimitePix);
            ps.executeUpdate();
            System.out.println("Financeiro inserido com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao inserir financeiro: " + e.getMessage());
        }
    }

    public void inserirMovimentacoes(Bank_DCV.WsMov[] movimentacoes, String cpf, int qtdMov) {
        String sql = "INSERT INTO movimentacao (cpf, tipo_mov, valor, hora, status_mov) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < qtdMov; i++) {
                ps.setString(1, cpf);
                ps.setString(2, movimentacoes[i].wsTipoMov.name());
                ps.setDouble(3, movimentacoes[i].wsValorMov);
                ps.setString(4, movimentacoes[i].wsHoraMov);
                ps.setString(5, movimentacoes[i].wsStatusMov.name());
                ps.executeUpdate();
            }
            System.out.println("Movimentacoes inseridas com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao inserir movimentacoes: " + e.getMessage());
        }
    }
}
