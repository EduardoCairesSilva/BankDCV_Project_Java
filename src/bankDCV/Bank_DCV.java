package bankDCV;

import java.sql.Connection;
import java.util.Scanner;

public class Bank_DCV {
	enum TipoConta{
		CC,
		CP,
		CS,
	}
	enum TipoMov{
		PIX,
		TED,
		DOC,
		SAQ,
		DEP,
	}
	enum StatusMov{
		A,
		N,
		P,
	}
	
	static class WsCliente{
		String wsCpf;
		String wsNome;
		String wsAgencia;
		String wsConta;
		int wsDigito;
		TipoConta wsTipoConta;
	}
	
	static class WsFinanceiro{
		double wsSaldoAtual;
		double wsLimiteCheque;
		double wsSaldoTotal;
		double wsLimitePix;
	}
	
	static class WsMov{
		TipoMov wsTipoMov;
		double wsValorMov;
		String wsHoraMov;
		StatusMov wsStatusMov;
	}
	
	static class WsControle{
		int wsIdx = 0;
		int wsTotAprovadas = 0;
		int wsTotNegadas = 0;
		double wsSomaDebitos = 0.0;
		double wsSomaCreditos = 0.0;
		int wsRetorno = 0;
	}
	
	static class WsCodigos{
		final int wsCodOk = 0;
		final int wsCodSaldo = 1;
		final int wsCodLimite = 2;
		final int wsCodBloqueio = 3;
		String wsContaBloq = "N";
		boolean contaBloqueada() {
			return wsContaBloq.equals("S");
		}
	}
	static WsCliente wsCliente = new WsCliente();
	static WsFinanceiro wsFinanceiro = new WsFinanceiro();
	static WsMov[] wsMovimentacoes = new WsMov[10];
	static WsControle wsControle = new WsControle();
	static WsCodigos wsCodigos = new WsCodigos();
	
	public static void main(String args[]) {
		Scanner scanner = new Scanner(System.in).useLocale(java.util.Locale.US);
		System.out.print("CPF (ex: 012.345.678-09): ");
		wsCliente.wsCpf = scanner.nextLine();
		System.out.print("Nome: ");
		wsCliente.wsNome = scanner.nextLine();
		System.out.print("Agencia: ");
		wsCliente.wsAgencia = scanner.nextLine();
		System.out.print("Conta: ");
		wsCliente.wsConta = scanner.nextLine();
		System.out.print("Digito: ");
		wsCliente.wsDigito = scanner.nextInt();
		scanner.nextLine();
		System.out.print("Tipo de conta (CC/CP/CS): ");
		wsCliente.wsTipoConta = TipoConta.valueOf(scanner.nextLine().toUpperCase());
		
		wsFinanceiro.wsSaldoAtual = 1500.00;
		wsFinanceiro.wsLimiteCheque = 500.00;
		wsFinanceiro.wsLimitePix = 1000.00;
		wsFinanceiro.wsSaldoTotal = wsFinanceiro.wsSaldoAtual + wsFinanceiro.wsLimiteCheque;
		
		System.out.print("Quantas movimentacoes deseja cadastrar? (max 10): ");
		int qtdMov = scanner.nextInt();
		scanner.nextLine();
		
		for(int i = 0; i < qtdMov; i++) {
			wsMovimentacoes[i] = new WsMov();
		}
		
		for (int i = 0; i < qtdMov; i++) {
		    System.out.println("\n--- Movimentacao " + (i + 1) + " ---");

		    System.out.print("Tipo (PIX/TED/DOC/SAQ/DEP): ");
		    wsMovimentacoes[i].wsTipoMov = TipoMov.valueOf(scanner.nextLine().toUpperCase());

		    System.out.print("Valor (negativo para debito, ex: -200.00): ");
		    wsMovimentacoes[i].wsValorMov = scanner.nextDouble();
		    scanner.nextLine();

		    System.out.print("Hora (ex: 083022): ");
		    wsMovimentacoes[i].wsHoraMov = scanner.nextLine();

		    wsMovimentacoes[i].wsStatusMov = StatusMov.P;
		}
		
		Connection conn = ConexaoPostgres.conectar();
	    DcvDAO dao = new DcvDAO(conn);
	    dao.inserirCliente(wsCliente);
	    dao.inserirFinanceiro(wsFinanceiro, wsCliente.wsCpf);
	    dao.inserirMovimentacoes(wsMovimentacoes, wsCliente.wsCpf, qtdMov);
		
		for(wsControle.wsIdx = 0; wsControle.wsIdx < qtdMov; wsControle.wsIdx++) {
			if(wsCodigos.contaBloqueada()) {
				wsControle.wsRetorno = wsCodigos.wsCodBloqueio;
				wsMovimentacoes[wsControle.wsIdx].wsStatusMov = StatusMov.N;
				wsControle.wsTotNegadas += 1;
				continue;
			}
			
			if(wsMovimentacoes[wsControle.wsIdx].wsValorMov < 0) {
				if(Math.abs(wsMovimentacoes[wsControle.wsIdx].wsValorMov) > wsFinanceiro.wsSaldoTotal) {
					wsControle.wsRetorno = wsCodigos.wsCodSaldo;
					wsMovimentacoes[wsControle.wsIdx].wsStatusMov = StatusMov.N;
					wsControle.wsTotNegadas += 1;
				} else if(wsMovimentacoes[wsControle.wsIdx].wsTipoMov == TipoMov.PIX && 
						Math.abs(wsMovimentacoes[wsControle.wsIdx].wsValorMov) > wsFinanceiro.wsLimitePix) {
					wsControle.wsRetorno = wsCodigos.wsCodLimite;
					wsMovimentacoes[wsControle.wsIdx].wsStatusMov = StatusMov.N;
					wsControle.wsTotNegadas += 1;
				} else {
					wsFinanceiro.wsSaldoAtual += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
					wsControle.wsSomaDebitos += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
					wsControle.wsRetorno = wsCodigos.wsCodOk;
					wsMovimentacoes[wsControle.wsIdx].wsStatusMov = StatusMov.A;
					wsControle.wsTotAprovadas += 1;
				}
			} else {
				wsFinanceiro.wsSaldoAtual += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
				wsControle.wsSomaCreditos += wsMovimentacoes[wsControle.wsIdx].wsValorMov;
				wsControle.wsRetorno = wsCodigos.wsCodOk;
				wsMovimentacoes[wsControle.wsIdx].wsStatusMov = StatusMov.A;
				wsControle.wsTotAprovadas += 1;
			}
			wsFinanceiro.wsSaldoTotal = wsFinanceiro.wsSaldoAtual + wsFinanceiro.wsLimiteCheque;
		}
		
		System.out.println("==============================");
		System.out.println("Extrato DCV - " + wsCliente.wsNome);
		System.out.println("AG: " + wsCliente.wsAgencia + " CC: " + wsCliente.wsConta + "-" + wsCliente.wsDigito);
		System.out.println("==============================");
		System.out.println("Saldo final: " + wsFinanceiro.wsSaldoAtual);
		System.out.println("Total Creditos: " + wsControle.wsSomaCreditos);
		System.out.println("Total Debitos: " + wsControle.wsSomaDebitos);
		System.out.println("Aprovadas: " + wsControle.wsTotAprovadas);
		System.out.println("Negadas: " + wsControle.wsTotNegadas);
		System.out.println("==============================");
	}
}
