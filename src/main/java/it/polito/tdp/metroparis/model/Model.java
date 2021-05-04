package it.polito.tdp.metroparis.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

import it.polito.tdp.metroparis.db.MetroDAO;

public class Model {
	
	Graph<Fermata, DefaultEdge> grafo; //vertici di tipo Fermata (con hashCode e equals necessariamente implementati), archi non pesati
	
	Map<Fermata, Fermata> predecessore; // valore predecessore della chiave
	
	public void creaGrafo() {
		this.grafo = new SimpleGraph<>(DefaultEdge.class); //passare al costruttore la classe degli archi
		
		MetroDAO dao = new MetroDAO();
		List<Fermata> fermate = dao.getAllFermate();
		
//		for(Fermata f : fermate) {
//			this.grafo.addVertex(f);
//		}
		
		Graphs.addAllVertices(this.grafo, fermate); //interfaccia con solo metodi statici utili
		
		//Aggiungiamo gli archi
		
//		for(Fermata f1 : this.grafo.vertexSet()) { 
//			for(Fermata f2 : this.grafo.vertexSet()) {
//				if(!f1.equals(f2) && dao.fermateCollegate(f1,f2)) {
//					this.grafo.addEdge(f1, f2);
//				}
//				
//			}
//		}
//		
		List<Connessione> connessioni = dao.getAllConnessioni(fermate);
		for(Connessione c : connessioni) {
			this.grafo.addEdge(c.getStazP(), c.getStazA());
		}
		
		System.out.format("Grafo creato con %d vertici e %d archi\n",
				this.grafo.vertexSet().size(), this.grafo.edgeSet().size()) ;
		
		//System.out.println(grafo);
		
//		Fermata f;
//		Set <DefaultEdge> archi = this.grafo.edgesOf(f); //trova tutti gli archi adiacenti ad un certo vertice
		
//		for(DefaultEdge e : archi) {
//			Fermata f1 = this.grafo.getEdgeSource(e);
//			//oppure (grafo non orientato)
//			Fermata f2 = this.grafo.getEdgeTarget(e);
//			if(f1.equals(f)) {
//				// f2 è quello che mi serve
//			} else {
//				// f1 è quello che mi serve
//			}
			
//			f1 = Graphs.getOppositeVertex(this.grafo, e, f); // dà il vertice opposto a f, collegato con e
//		}
		
//		List<Fermata> fermateAdiacenti = Graphs.successorListOf(this.grafo, f); //ritorna i vertici adiacenti a f, senza passare dagli archi come il metodo precedente (più comodo)
			
	}
	
	
	public List<Fermata> fermateRaggiungibili(Fermata partenza){
		
		// visita in profondità
		
		//DepthFirstIterator<Fermata, DefaultEdge> dfv = new DepthFirstIterator<>(this.grafo, partenza);
		
		// visita in ampiezza
		
		BreadthFirstIterator<Fermata, DefaultEdge> bfv = new BreadthFirstIterator<>(this.grafo, partenza);
		
		this.predecessore = new HashMap<>();
		this.predecessore.put(partenza, null);
		
		// aggiungo all'iteratore un listener che lo ascolti e ritorni qualcosa ogni volta che l'iteratore scatena un evento
		
		bfv.addTraversalListener(new TraversalListener<Fermata, DefaultEdge>(){ //creazione di una classe "inline" a partire dall'interfaccia e creazione di un suo oggetto con new
			
			// questi metodi agiscono ogni volta che viene attraversato un arco/vertice
			
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
			
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {

			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultEdge> e) { // evento ogni volta che l'algoritmo attraversa un nuovo arco (nota bene: se il vertice di destinazione era già noto, l'arco non verrà però aggiunto alla soluzione finale)
				DefaultEdge arco = e.getEdge();
				Fermata a = grafo.getEdgeSource(arco); // source e target si riferiscono al grafo così com'è stato creato, non alla direzione di visita, quindi perdono un po' di significato
				Fermata b = grafo.getEdgeTarget(arco);
				
				// ho scoperto 'a' arrivando da 'b' (se 'b' lo conoscevo)
				
				if(predecessore.containsKey(b) && !predecessore.containsKey(a)) {
					predecessore.put(a, b);
					//System.out.println(a+" scoperto da "+b);
				} else if(predecessore.containsKey(a) && !predecessore.containsKey(b)) {
					// di sicuro conoscevo 'a' e quindi ho scoperto 'b'
					predecessore.put(b, a);
				//	System.out.println(b+" scoperto da "+a);
				}
				
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Fermata> e) {
				//System.out.println(e.getVertex());
//				
//				Fermata nuova = e.getVertex();
//				Fermata precedente; // vertice adiacente a 'nuova' che sia già stato raggiunto (cioè è già presente nelle key della mappa)
//				predecessore.put(nuova, precedente);
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Fermata> e) {

			}
			
		});
		
		List<Fermata> result = new ArrayList<>();
		
		while(bfv.hasNext()) {
			Fermata f = bfv.next();
			result.add(f);
		}
		
		return result;
	}
	
	public Fermata trovaFermata(String nome) {
		for(Fermata f : this.grafo.vertexSet()) {
			if(f.getNome().equals(nome)) {
				return f;
			}
		}
		
		return null;
	}
	
	public List<Fermata> trovaCammino(Fermata partenza, Fermata arrivo){
		fermateRaggiungibili(partenza);
		
		List<Fermata> result = new LinkedList<>();
		result.add(arrivo);
		Fermata f = arrivo; //segnaposto iniziale sul vertice di arrivo
		
		while(predecessore.get(f)!=null) {
			f = predecessore.get(f);
			result.add(0,f); // aggiungo in testa per rispettare l'ordine del cammino --> meglio usare LinkedList
		}
		
		return result;
	}
	
	// Implementazione di 'trovaCammino' che NON usa il traversal listener ma sfrutta
	// il metodo getParent presente in BreadthFirstIterator
	public List<Fermata> trovaCammino2(Fermata partenza, Fermata arrivo) {
		BreadthFirstIterator<Fermata, DefaultEdge> bfv = 
				new BreadthFirstIterator<Fermata, DefaultEdge>(this.grafo, partenza) ;
		
		// fai lavorare l'iteratore per trovare tutti i vertici
		while(bfv.hasNext())
			bfv.next() ; // non mi serve il valore
		
		List<Fermata> result = new LinkedList<>() ;
		Fermata f = arrivo ;
		while(f!=null) {
			result.add(f) ;
			f = bfv.getParent(f) ;
		}
		
		return result ;
		
	}

}
