package org.fog.heuristics.fogImplementations;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.heuristics.algorithms.ga.GeneticAlgorithm;
import org.fog.heuristics.fogImplementations.Utils.ListDevices;

/**
 * Utility class that both provides the so-called "mutations" (used in
 * {@link GeneticAlgorithm}) and a way to generate a random
 * {@link PieceOfSolution} that satisfy some design constraints (through
 * {@link #newRandomSolution(AppModule, List, Random)}).
 * <p>
 * Beware: its' NOT synchronized! Different instances must be created in order
 * to not mess up the results!
 */
public class SolutionMutatorFog implements BiFunction<PieceOfSolution, Random, PieceOfSolution> {

	@Override
	public PieceOfSolution apply(PieceOfSolution oldSolution, Random r) {
		return Utils.newRandomPieceOfSolution(oldSolution.getModule(), getDevicesPartitions(),
				getApplicationsSubmitted(), getDevices(), r);
	}

	protected Map<String, Application> applicationsSubmitted;
	protected List<AppModule> modules;
	protected List<FogDevice> devices;
	protected ListDevices[] devicesPartitions;

	public void resetEvolutionEnvironment(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices) {
		resetEvolutionEnvironment(applicationsSubmitted, modules, devices, Utils.partitionateDevicesByType(devices));
	}

	public void resetEvolutionEnvironment(Map<String, Application> applicationsSubmitted, List<AppModule> modules,
			List<FogDevice> devices, ListDevices[] devicesPartitions) {
		this.applicationsSubmitted = applicationsSubmitted;
		this.modules = modules;
		this.devices = devices;
		this.devicesPartitions = devicesPartitions;
	}

	public Map<String, Application> getApplicationsSubmitted() {
		return applicationsSubmitted;
	}

	public List<AppModule> getModules() {
		return modules;
	}

	public List<FogDevice> getDevices() {
		return devices;
	}

	public ListDevices[] getDevicesPartitions() {
		return devicesPartitions;
	}
}
