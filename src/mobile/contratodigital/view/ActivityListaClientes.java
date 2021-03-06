package mobile.contratodigital.view;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import mobile.contratodigital.R;
import mobile.contratodigital.dao.Dao;
import mobile.contratodigital.model.ContratoUtil;
import mobile.contratodigital.model.ItemPeca;
import mobile.contratodigital.util.PermissaoActivity;
import mobile.contratodigital.util.TelaBuilder;
import mobile.contratodigital.util.TrabalhaComArquivos;
import sharedlib.contratodigital.model.*;
import sharedlib.contratodigital.util.Generico;

public class ActivityListaClientes extends Activity {

	private Context context;
	private ActionBar actionBar;
	private ListView listView;
	private List<Movimento> listaComMovimentos;	
	private Dao dao;
	private ArrayAdapterCliente adapterCliente;
	private Layout layout;
	//private PermissaoActivity permissaoActivity;
	private static final int REQUISICAO_PERMISSAO_ESCRITA = 333;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = ActivityListaClientes.this;
			
		dao = new Dao(context);
		
		layout = (Layout) dao.devolveObjeto(Layout.class, 
											Layout.COLUMN_INTEGER_OBRIGATORIO, Generico.LAYOUT_OBRIGATORIO_SIM.getValor());
		
		if(layout == null){
			
			listaComMovimentos = new ArrayList<Movimento>();
		}
		else{
			listaComMovimentos = dao.listaTodaTabela_GroupBy_NrVisita(Movimento.class, layout.getNr_layout());
		}	
		
	 	adapterCliente = new ArrayAdapterCliente(context, R.layout.adapter_cliente, listaComMovimentos);

	 	//permissaoActivity = new PermissaoActivity();
	 	
		actionBar = getActionBar();
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(getString(R.color.azul_consigaz))));
		actionBar.setTitle("Lista de Clientes");		

		setContentView(constroiTelaInicial());
	}
	
	private LinearLayout constroiTelaInicial(){

		TelaBuilder telaBuilder = new TelaBuilder(context);
		
		LayoutParams layoutParams_MATCH_MATCH = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		LinearLayout linearLayoutTela = telaBuilder.cria_LL(layoutParams_MATCH_MATCH, R.color.plano_de_fundo_layout);
		
		listView = telaBuilder.cria_LV();
		listView.setAdapter(adapterCliente);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				for (Movimento movimento : listaComMovimentos) {

					if (movimento.getNr_visita() == view.getId()) {
						
						abreFragActivityOcorrencia(movimento, view.getId());
						
						break;
					}
				}
			}
		});
			
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView parent, View view, int position, long id) {
					
				acaoAposLongClick(view);
				  
			return true;
			}
		});
		
		LinearLayout linearLayout_holderListView = telaBuilder.cria_LL_HOLDER(0.06f);		
					 		linearLayout_holderListView.addView(listView);
		linearLayoutTela.addView(linearLayout_holderListView);
		
		LinearLayout linearLayout_holderButton = telaBuilder.cria_LL_HOLDER(0.94f);
		
		Button button_cadastrarNovoCliente = telaBuilder.cria_BT_paraListaDeVendas("Cadastrar Novo Cliente");
			   button_cadastrarNovoCliente.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				acaoAposCliqueNoBotaoCadastrarNovoCliente();
			}
		});
					 linearLayout_holderButton.addView(button_cadastrarNovoCliente);
		linearLayoutTela.addView(linearLayout_holderButton);
		
		return linearLayoutTela;
	}

	private void acaoAposLongClick(View view){
		
		for (Movimento movimento : listaComMovimentos) {

			if (movimento.getNr_visita() == view.getId()) {
				
				solicitaConfirmacaoDeletarCliente(movimento);
				
				break;
			}
		}
	}
	
	private void solicitaConfirmacaoDeletarCliente(final Movimento movimento) {
		
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		   alertDialog.setTitle("Aten��o");
	       alertDialog.setMessage("Deseja deletar o cliente "+movimento.getInformacao_1()+" ?");
	       alertDialog.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					
					ContratoUtil contratoUtil = new ContratoUtil(dao, context);

					if (Build.VERSION.SDK_INT >= 23) {						
						if (permitiuEscrever()){
							
							contratoUtil.deletaCliente(movimento, listaComMovimentos, adapterCliente);
						}		
				    } 
					else {
						contratoUtil.deletaCliente(movimento, listaComMovimentos, adapterCliente);
				    }
					
				}
			});
	        alertDialog.setNegativeButton("N�o", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
				}
			});	        
	        alertDialog.show();
	}
	
	@SuppressLint("NewApi")
	public boolean permitiuEscrever(){
        
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            
        	requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUISICAO_PERMISSAO_ESCRITA);		      
 		
            return false;
        }
        return true;
    }

	private void acaoAposCliqueNoBotaoCadastrarNovoCliente(){
	
		Movimento movimento = new Movimento();
		 
		if(listaComMovimentos.size() == 0){
	  		
			movimento.setNr_programacao(1);				
		}else{					
			movimento.setNr_programacao(listaComMovimentos.get(0).getNr_programacao());
		}
		
		int ultimoNrVisita = dao.devolveUltimoNrVisitaDoMovimento();
		
		int ultimoNrVisita_mais1 = ultimoNrVisita + 1;
					  	
		  	movimento.setNr_visita(ultimoNrVisita_mais1);

		abreFragActivityOcorrencia(movimento, ultimoNrVisita_mais1);
	}
	
	private void abreFragActivityOcorrencia(Movimento movimento, int viewId){
		
		Bundle bundle = new Bundle();	
	   	       bundle.putSerializable("movimento", movimento);

	   	Intent intent = new Intent(context, FragActivityOcorrencia.class);
    	   	   intent.putExtras(bundle);

    	startActivityForResult(intent, viewId);		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		listaComMovimentos = dao.listaTodaTabela_GroupBy_NrVisita(Movimento.class, layout.getNr_layout());	

		if (intent != null) {
		
			for (Movimento movimento : listaComMovimentos) {

				if (movimento.getNr_visita() == requestCode) {
					
					if (resultCode == Generico.PREENCHEU_FORMULARIO_OBRIGATRORIOS_SIM.getValor()) {

						movimento.setStatus(Generico.REALIZADA.getValor());

						dao.insereOUatualiza(movimento,
											 //Movimento.COLUMN_INTEGER_NR_PROGRAMACAO, movimento.getNr_programacao(), 
											 Movimento.COLUMN_INTEGER_NR_LAYOUT, movimento.getNr_layout(), 
											 Movimento.COLUMN_INTEGER_NR_VISITA, movimento.getNr_visita());
						
						finish();
						startActivity(getIntent());
					}
					
					if (resultCode == Generico.PREENCHEU_FORMULARIO_OBRIGATRORIOS_NAO.getValor()) {

						movimento.setStatus(Generico.NAO_REALIZADA.getValor());

						dao.insereOUatualiza(movimento, 
								 			 //Movimento.COLUMN_INTEGER_NR_PROGRAMACAO, movimento.getNr_programacao(), 
								 			 Movimento.COLUMN_INTEGER_NR_LAYOUT, movimento.getNr_layout(), 
								 			 Movimento.COLUMN_INTEGER_NR_VISITA, movimento.getNr_visita());						
					}
					
					break;
				}			
			}
			//adapter.notifyDataSetChanged();			
		}
	}

	@Override
	public void onBackPressed() {

		startActivity(new Intent(ActivityListaClientes.this, ActivityDashboard.class));
		finish();
	}

}
