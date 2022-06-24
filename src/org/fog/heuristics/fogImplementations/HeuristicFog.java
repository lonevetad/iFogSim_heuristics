package org.fog.heuristics.fogImplementations;

import org.fog.heuristics.Heuristic;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 */
public interface HeuristicFog extends Heuristic<SolutionModulesDeployed> {

	public ModulePlacementAdditionalInformationFog getModPlacementAdditionalInfo();

	public void setModPlacementAdditionalInfo(ModulePlacementAdditionalInformationFog modPlacementAdditionalInfo);
}
