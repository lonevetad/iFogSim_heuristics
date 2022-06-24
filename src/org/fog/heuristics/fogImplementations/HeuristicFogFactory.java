package org.fog.heuristics.fogImplementations;

import java.util.function.Function;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 * 
 */
public interface HeuristicFogFactory extends Function<ModulePlacementAdditionalInformationFog, HeuristicFog> {
	public <S extends SolutionModulesDeployed> HeuristicFog newInstance(
			ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo, SolutionMutatorFog<S> mutator);

	public default HeuristicFog newInstance(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		return this.newInstance(modPlacementAdditionalInfo, null);
	}

	@Override
	public default HeuristicFog apply(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo) {
		return this.newInstance(modPlacementAdditionalInfo);
	}
}
