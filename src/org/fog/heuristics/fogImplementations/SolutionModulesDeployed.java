package org.fog.heuristics.fogImplementations;

import java.io.Serializable;
import java.util.List;

import org.fog.heuristics.Heuristic;
import org.fog.placement.ModulePlacement;

/**
 * It's a solution of a module-placement optimization problem (a "decision",
 * kind of), the one that {@link Heuristic}s will come up with and the
 * {@link ModulePlacement} has to provide and use.<br>
 * That's a
 */
public class SolutionModulesDeployed implements Serializable {
	private static final long serialVersionUID = 1L;
//protected AppModule moduleToDepoy;
//protected Application applicationOfTheModule;
//protected  FogDevice 

	public SolutionModulesDeployed() {
		super();
	}

	public SolutionModulesDeployed(List<PieceOfSolution> pieces) {
		this();
		this.setPieces(pieces);
	}

	protected List<PieceOfSolution> pieces;

	public List<PieceOfSolution> getPieces() {
		return pieces;
	}

	public void setPieces(List<PieceOfSolution> pieces) {
		this.pieces = pieces;
	}

	//

	/*
	 * protected static interface EffectiveAdderPieceOfSolution{ public void
	 * addEffectively(PieceOfSolution pos); } protected static class MapAligned<A,B>
	 * extends TreeMap<A,B> implements EffectiveAdderPieceOfSolution{ private static
	 * final long serialVersionUID = -650425400520L;
	 * 
	 * @Override public void addEffectively(PieceOfSolution pos) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 * 
	 * }
	 */
}
