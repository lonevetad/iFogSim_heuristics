package org.fog.heuristics.fogImplementations.sa;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.fog.entities.FogDevice;
import org.fog.heuristics.SolutionsProducerEvaluator.CumulatedCostsOnDevice;
import org.fog.heuristics.SolutionsProducerEvaluator.SolutionDeployCosts;
import org.fog.heuristics.fogImplementations.ModulePlacementAdditionalInformationFog;
import org.fog.heuristics.fogImplementations.PieceOfSolution;
import org.fog.heuristics.fogImplementations.SolutionModulesDeployed;
import org.fog.heuristics.fogImplementations.SolutionMutatorFog;

public class SASolutionFog implements Cloneable, Supplier<List<PieceOfSolution>>, SolutionModulesDeployed {
	private static final long serialVersionUID = 34647856001025L;
	protected List<PieceOfSolution> pieces;
	protected SolutionDeployCosts<SASolutionFog> costs;

	public SASolutionFog() {
		super();
	}

	public SASolutionFog(List<PieceOfSolution> pieces) {
		super();
		this.setPieces(pieces);
	}

	@Override
	public List<PieceOfSolution> getPieces() {
		return pieces;
	}

	@Override
	public void setPieces(List<PieceOfSolution> pieces) {
		this.pieces = pieces;
		this.costs = new SolutionDeployCosts<>(this);
	}

	/**
	 * @return the costs
	 */
	public SolutionDeployCosts<SASolutionFog> getCosts() {
		return costs;
	}

	@Override
	public List<PieceOfSolution> get() {
		return pieces;
	}

	public SASolutionFog randomWalk(Random r, SolutionMutatorFog<SASolutionFog> mutator,
			ModulePlacementAdditionalInformationFog additionalInformation) {
		int indexFlip;
		List<PieceOfSolution> newList;
		PieceOfSolution posToChange;
		FogDevice originalDevice;

		indexFlip = r.nextInt(this.pieces.size());
		newList = new ArrayList<>();
		posToChange = null;
		for (int i = 0, s = pieces.size(); i < s; i++) {
			PieceOfSolution clonedPos;
			clonedPos = (PieceOfSolution) this.pieces.get(i).clone();
			if (i == indexFlip) {
				posToChange = clonedPos;
			}
			newList.add(clonedPos);
		}

		originalDevice = posToChange.getDevice();
		posToChange = mutator.mutateFragmentOfSolution(posToChange, this, null, r);
		if (mutator.hasSolutionCost(this)) {
			SolutionDeployCosts<SASolutionFog> cost;
			CumulatedCostsOnDevice newCost;

			cost = mutator.getCostSolution(this);
			cost.remove(originalDevice);
			newCost = new CumulatedCostsOnDevice(posToChange.getDevice());
			newCost.accumulateCostsOf(posToChange.getModule(),
					additionalInformation.getApplicationsByID().get(posToChange.getModule().getAppId()));
			cost.addCumulatedCosts(newCost);
		}

		newList.set(indexFlip, posToChange);

		return new SASolutionFog(newList);
	}

	@Override
	public Object clone() {
		List<PieceOfSolution> p;
		p = new ArrayList<>(pieces.size());
		this.pieces.forEach(pos -> p.add((PieceOfSolution) pos.clone()));
		return new SASolutionFog(p);
	}
}
