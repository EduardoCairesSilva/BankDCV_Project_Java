package bankDCV;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DcvGUI extends JFrame {

    static Bank_DCV.WsCliente wsCliente         = new Bank_DCV.WsCliente();
    static Bank_DCV.WsFinanceiro wsFinanceiro   = new Bank_DCV.WsFinanceiro();
    static Bank_DCV.WsMov[] wsMovimentacoes     = new Bank_DCV.WsMov[10];
    static Bank_DCV.WsControle wsControle       = new Bank_DCV.WsControle();
    static Bank_DCV.WsCodigos wsCodigos         = new Bank_DCV.WsCodigos();
    static int qtdMov = 0;

    private JPanel painelPrincipal;
    private CardLayout cardLayout;

    private JTextField txtCpf, txtNome, txtAgencia, txtConta, txtDigito;
    private JTextField txtSaldoAtual, txtLimiteCheque, txtLimitePix;
    private JComboBox<String> cbTipoConta;

    private JPanel painelMovimentacoes;
    private JSpinner spinnerQtd;
    private JPanel painelLinhasMov;
    private JComboBox<String>[] cbTipoMov;
    private JTextField[] txtValorMov, txtHoraMov;

    private JTextArea txtExtrato;

    private JTable tabelaClientes;
    private DefaultTableModel modeloTabela;

    @SuppressWarnings("unchecked")
    public DcvGUI() {
        setTitle("DCV Bancário");
        setSize(700, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);

        painelPrincipal.add(criarTelaCliente(),        "CLIENTE");
        painelPrincipal.add(criarTelaMovimentacoes(),  "MOVIMENTACOES");
        painelPrincipal.add(criarTelaExtrato(),        "EXTRATO");
        painelPrincipal.add(criarTelaListaClientes(),  "LISTA");

        add(painelPrincipal);
        add(criarMenuNavegacao(), BorderLayout.SOUTH);
    }

    private JPanel criarTelaCliente() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titulo = new JLabel("Cadastro de Cliente", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        painel.add(titulo, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(9, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        txtCpf = new JTextField();
        txtNome = new JTextField();
        txtAgencia = criarCampoNumerico(4);
        txtConta   = criarCampoNumerico(7);
        txtDigito  = criarCampoNumerico(1);
        txtSaldoAtual = new JTextField("1500.00");
        txtLimiteCheque = new JTextField("500.00");
        txtLimitePix = new JTextField("1000.00");
        cbTipoConta = new JComboBox<>(new String[]{"CC", "CP", "CS"});
        
        txtCpf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            boolean formatando = false;

            public void insertUpdate(javax.swing.event.DocumentEvent e) { formatar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {}
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}

            private void formatar() {
                if (formatando) return;
                formatando = true;
                SwingUtilities.invokeLater(() -> {
                    String texto = txtCpf.getText().replaceAll("[^0-9]", "");
                    if (texto.length() > 11) texto = texto.substring(0, 11);

                    StringBuilder sb = new StringBuilder(texto);
                    if (sb.length() > 9) sb.insert(9, '-');
                    if (sb.length() > 6) sb.insert(6, '.');
                    if (sb.length() > 3) sb.insert(3, '.');

                    txtCpf.setText(sb.toString());
                    txtCpf.setCaretPosition(txtCpf.getText().length());
                    formatando = false;
                });
            }
        });

        form.add(new JLabel("CPF (ex: 012.345.678-09):")); form.add(txtCpf);
        form.add(new JLabel("Nome:"));                     form.add(txtNome);
        form.add(new JLabel("Agência:"));                  form.add(txtAgencia);
        form.add(new JLabel("Conta:"));                    form.add(txtConta);
        form.add(new JLabel("Dígito:"));                   form.add(txtDigito);
        form.add(new JLabel("Saldo Atual:"));              form.add(txtSaldoAtual);
        form.add(new JLabel("Limite Cheque:"));            form.add(txtLimiteCheque);
        form.add(new JLabel("Limite PIX:"));               form.add(txtLimitePix);
        form.add(new JLabel("Tipo de Conta:"));            form.add(cbTipoConta);

        painel.add(form, BorderLayout.CENTER);

        JButton btnProximo = new JButton("Próximo →");
        btnProximo.setBackground(new Color(0, 120, 215));
        btnProximo.setForeground(Color.WHITE);
        btnProximo.setFont(new Font("Arial", Font.BOLD, 13));
        btnProximo.addActionListener(e -> salvarCliente());
        painel.add(btnProximo, BorderLayout.SOUTH);

        return painel;
    }
    
    private JTextField criarCampoNumerico(int maxDigitos) {
        JTextField campo = new JTextField();
        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            boolean formatando = false;

            public void insertUpdate(javax.swing.event.DocumentEvent e) { limitar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {}
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}

            private void limitar() {
                if (formatando) return;
                formatando = true;
                SwingUtilities.invokeLater(() -> {
                    String texto = campo.getText().replaceAll("[^0-9]", "");
                    if (texto.length() > maxDigitos) texto = texto.substring(0, maxDigitos);
                    campo.setText(texto);
                    campo.setCaretPosition(campo.getText().length());
                    formatando = false;
                });
            }
        });
        return campo;
    }

    @SuppressWarnings("unchecked")
    private JPanel criarTelaMovimentacoes() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titulo = new JLabel("Cadastro de Movimentações", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        painel.add(titulo, BorderLayout.NORTH);

        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Quantas movimentações? (max 10):"));
        spinnerQtd = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        topo.add(spinnerQtd);
        JButton btnGerar = new JButton("Gerar campos");
        btnGerar.addActionListener(e -> gerarCamposMov());
        topo.add(btnGerar);

        painelLinhasMov = new JPanel();
        painelLinhasMov.setLayout(new BoxLayout(painelLinhasMov, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(painelLinhasMov);

        JButton btnProcessar = new JButton("Processar e Ver Extrato →");
        btnProcessar.setBackground(new Color(0, 150, 0));
        btnProcessar.setForeground(Color.WHITE);
        btnProcessar.setFont(new Font("Arial", Font.BOLD, 13));
        btnProcessar.addActionListener(e -> processarMovimentacoes());

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(topo, BorderLayout.NORTH);
        centro.add(scroll, BorderLayout.CENTER);

        painel.add(centro, BorderLayout.CENTER);
        painel.add(btnProcessar, BorderLayout.SOUTH);

        return painel;
    }

    @SuppressWarnings("unchecked")
    private void gerarCamposMov() {
        qtdMov = (int) spinnerQtd.getValue();
        cbTipoMov  = new JComboBox[qtdMov];
        txtValorMov = new JTextField[qtdMov];
        txtHoraMov  = new JTextField[qtdMov];

        painelLinhasMov.removeAll();

        for (int i = 0; i < qtdMov; i++) {
            JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT));
            linha.setBorder(BorderFactory.createTitledBorder("Movimentação " + (i + 1)));

            cbTipoMov[i]   = new JComboBox<>(new String[]{"PIX","TED","DOC","SAQ","DEP"});
            txtValorMov[i] = new JTextField(10);
            txtHoraMov[i] = new JTextField(
            	    new java.text.SimpleDateFormat("HHmmss").format(new java.util.Date()));
            txtHoraMov[i].setEditable(false);
            txtHoraMov[i].setBackground(new Color(220, 220, 220));
            txtHoraMov[i].setPreferredSize(new Dimension(80, 25));

            linha.add(new JLabel("Tipo:"));  linha.add(cbTipoMov[i]);
            linha.add(new JLabel("Valor:")); linha.add(txtValorMov[i]);
            linha.add(new JLabel("Hora:"));  linha.add(txtHoraMov[i]);

            painelLinhasMov.add(linha);
        }

        painelLinhasMov.revalidate();
        painelLinhasMov.repaint();
    }

    private JPanel criarTelaExtrato() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titulo = new JLabel("Extrato DCV", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        painel.add(titulo, BorderLayout.NORTH);

        txtExtrato = new JTextArea();
        txtExtrato.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtExtrato.setEditable(false);
        painel.add(new JScrollPane(txtExtrato), BorderLayout.CENTER);

        JButton btnNovo = new JButton("+ Novo Cliente");
        btnNovo.setBackground(new Color(0, 120, 215));
        btnNovo.setForeground(Color.WHITE);
        btnNovo.setFont(new Font("Arial", Font.BOLD, 13));
        btnNovo.addActionListener(e -> novoCliente());
        painel.add(btnNovo, BorderLayout.SOUTH);

        return painel;
    }

    private JPanel criarTelaListaClientes() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titulo = new JLabel("Clientes Cadastrados", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        painel.add(titulo, BorderLayout.NORTH);

        String[] colunas = {"CPF", "Nome", "Agência", "Conta", "Tipo"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaClientes = new JTable(modeloTabela);
        painel.add(new JScrollPane(tabelaClientes), BorderLayout.CENTER);

        JButton btnAtualizar = new JButton("Atualizar lista");
        btnAtualizar.addActionListener(e -> carregarClientes());
        painel.add(btnAtualizar, BorderLayout.SOUTH);

        return painel;
    }

    private JPanel criarMenuNavegacao() {
        JPanel menu = new JPanel(new FlowLayout());
        menu.setBackground(new Color(40, 40, 40));

        String[] telas = {"Cliente", "Movimentações", "Extrato", "Clientes"};
        String[] ids   = {"CLIENTE", "MOVIMENTACOES", "EXTRATO", "LISTA"};

        for (int i = 0; i < telas.length; i++) {
            JButton btn = new JButton(telas[i]);
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(60, 60, 60));
            btn.setBorderPainted(false);
            final String id = ids[i];
            btn.addActionListener(e -> {
                if (id.equals("LISTA")) carregarClientes();
                cardLayout.show(painelPrincipal, id);
            });
            menu.add(btn);
        }
        return menu;
    }

    private void salvarCliente() {
        try {
            wsCliente.wsCpf       = txtCpf.getText().trim();
            wsCliente.wsNome      = txtNome.getText().trim();
            wsCliente.wsAgencia   = txtAgencia.getText().trim();
            wsCliente.wsConta     = txtConta.getText().trim();
            wsCliente.wsDigito    = Integer.parseInt(txtDigito.getText().trim());
            wsCliente.wsTipoConta = Bank_DCV.TipoConta.valueOf(cbTipoConta.getSelectedItem().toString());

            wsFinanceiro.wsSaldoAtual   = Double.parseDouble(txtSaldoAtual.getText().replace(",", "."));
            wsFinanceiro.wsLimiteCheque = Double.parseDouble(txtLimiteCheque.getText().replace(",", "."));
            wsFinanceiro.wsLimitePix    = Double.parseDouble(txtLimitePix.getText().replace(",", "."));
            wsFinanceiro.wsSaldoTotal   = wsFinanceiro.wsSaldoAtual + wsFinanceiro.wsLimiteCheque;

            wsControle = new Bank_DCV.WsControle();
            wsCodigos  = new Bank_DCV.WsCodigos();

            cardLayout.show(painelPrincipal, "MOVIMENTACOES");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro nos dados do cliente: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processarMovimentacoes() {
        try {
            if (cbTipoMov == null || cbTipoMov.length == 0) {
                JOptionPane.showMessageDialog(this, "Clique em 'Gerar campos' primeiro!");
                return;
            }

            wsMovimentacoes = new Bank_DCV.WsMov[10];
            for (int i = 0; i < qtdMov; i++) {
                wsMovimentacoes[i] = new Bank_DCV.WsMov();
                wsMovimentacoes[i].wsTipoMov  = Bank_DCV.TipoMov.valueOf(
                        cbTipoMov[i].getSelectedItem().toString());
                wsMovimentacoes[i].wsValorMov = Double.parseDouble(
                        txtValorMov[i].getText().replace(",", "."));
                wsMovimentacoes[i].wsHoraMov  = txtHoraMov[i].getText().trim();
                wsMovimentacoes[i].wsStatusMov = Bank_DCV.StatusMov.P;
            }

            Connection conn = ConexaoPostgres.conectar();
            DcvDAO dao = new DcvDAO(conn);
            dao.inserirCliente(wsCliente);
            dao.inserirFinanceiro(wsFinanceiro, wsCliente.wsCpf);
            dao.inserirMovimentacoes(wsMovimentacoes, wsCliente.wsCpf, qtdMov);

            for (wsControle.wsIdx = 0; wsControle.wsIdx < qtdMov; wsControle.wsIdx++) {
                if (wsCodigos.contaBloqueada()) {
                    wsControle.wsRetorno = wsCodigos.wsCodBloqueio;
                    wsMovimentacoes[wsControle.wsIdx].wsStatusMov = Bank_DCV.StatusMov.N;
                    wsControle.wsTotNegadas++;
                    continue;
                }
                if (wsMovimentacoes[wsControle.wsIdx].wsValorMov < 0) {
                    if (Math.abs(wsMovimentacoes[wsControle.wsIdx].wsValorMov) > wsFinanceiro.wsSaldoTotal) {
                        wsControle.wsRetorno = wsCodigos.wsCodSaldo;
                        wsMovimentacoes[wsControle.wsIdx].wsStatusMov = Bank_DCV.StatusMov.N;
                        wsControle.wsTotNegadas++;
                    } else if (wsMovimentacoes[wsControle.wsIdx].wsTipoMov == Bank_DCV.TipoMov.PIX &&
                            Math.abs(wsMovimentacoes[wsControle.wsIdx].wsValorMov) > wsFinanceiro.wsLimitePix) {
                        wsControle.wsRetorno = wsCodigos.wsCodLimite;
                        wsMovimentacoes[wsControle.wsIdx].wsStatusMov = Bank_DCV.StatusMov.N;
                        wsControle.wsTotNegadas++;
                    } else {
                        wsFinanceiro.wsSaldoAtual += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
                        wsControle.wsSomaDebitos  += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
                        wsControle.wsRetorno = wsCodigos.wsCodOk;
                        wsMovimentacoes[wsControle.wsIdx].wsStatusMov = Bank_DCV.StatusMov.A;
                        wsControle.wsTotAprovadas++;
                    }
                } else {
                    wsFinanceiro.wsSaldoAtual  += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
                    wsControle.wsSomaCreditos  += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
                    wsControle.wsRetorno = wsCodigos.wsCodOk;
                    wsMovimentacoes[wsControle.wsIdx].wsStatusMov = Bank_DCV.StatusMov.A;
                    wsControle.wsTotAprovadas++;
                }
                wsFinanceiro.wsSaldoTotal = wsFinanceiro.wsSaldoAtual + wsFinanceiro.wsLimiteCheque;
            }

            exibirExtrato();
            cardLayout.show(painelPrincipal, "EXTRATO");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao processar: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exibirExtrato() {
        StringBuilder sb = new StringBuilder();
        sb.append("==============================\n");
        sb.append("Extrato DCV - ").append(wsCliente.wsNome).append("\n");
        sb.append("AG: ").append(wsCliente.wsAgencia)
          .append(" CC: ").append(wsCliente.wsConta)
          .append("-").append(wsCliente.wsDigito).append("\n");
        sb.append("==============================\n");
        sb.append(String.format("Saldo final   : R$ %.2f%n", wsFinanceiro.wsSaldoAtual));
        sb.append(String.format("Total Creditos: R$ %.2f%n", wsControle.wsSomaCreditos));
        sb.append(String.format("Total Debitos : R$ %.2f%n", wsControle.wsSomaDebitos));
        sb.append("Aprovadas     : ").append(wsControle.wsTotAprovadas).append("\n");
        sb.append("Negadas       : ").append(wsControle.wsTotNegadas).append("\n");
        sb.append("==============================\n\n");
        sb.append("--- Detalhes ---\n");
        for (int i = 0; i < qtdMov; i++) {
            sb.append(String.format("[%s] %s  R$ %.2f  %s%n",
                    wsMovimentacoes[i].wsStatusMov,
                    wsMovimentacoes[i].wsTipoMov,
                    wsMovimentacoes[i].wsValorMov,
                    wsMovimentacoes[i].wsHoraMov));
        }
        txtExtrato.setText(sb.toString());
    }

    private void carregarClientes() {
        try {
            Connection conn = ConexaoPostgres.conectar();
            String sql = "SELECT cpf, nome, agencia, conta, tipo_conta FROM cliente";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            modeloTabela.setRowCount(0);
            while (rs.next()) {
                modeloTabela.addRow(new Object[]{
                    rs.getString("cpf"),
                    rs.getString("nome"),
                    rs.getString("agencia"),
                    rs.getString("conta"),
                    rs.getString("tipo_conta")
                });
            }
            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar clientes: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void novoCliente() {
        txtCpf.setText("");
        txtNome.setText("");
        txtAgencia.setText("");
        txtConta.setText("");
        txtDigito.setText("");
        txtSaldoAtual.setText("1500.00");
        txtLimiteCheque.setText("500.00");
        txtLimitePix.setText("1000.00");
        painelLinhasMov.removeAll();
        painelLinhasMov.revalidate();
        painelLinhasMov.repaint();
        cardLayout.show(painelPrincipal, "CLIENTE");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DcvGUI().setVisible(true);
        });
    }
}
