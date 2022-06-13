package org.fog.heuristics.fogImplementations;

import java.util.List;
import java.util.Map;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

public interface HeuristicFogFactory {
	public <S extends SolutionModulesDeployed> HeuristicFog newInstance(Map<String, Application> applicationsSubmitted,
			List<AppModule> modules, List<FogDevice> devices, SolutionMutatorFog<S> mutator);

	public default HeuristicFog newInstance(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices) {
		return this.newInstance(applicationsSubmitted, modules, devices, null);
	}
}
