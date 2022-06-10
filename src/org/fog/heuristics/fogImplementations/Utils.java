package org.fog.heuristics.fogImplementations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cloudbus.cloudsim.power.PowerHost;
import org.fog.application.AppModule;
import org.fog.application.AppModule.ModuleType;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDevice.DeviceNodeType;

public class Utils {

	/**
	 * TODO calculation missing:<br>
	 * <ul>
	 * <li>deployment time (d.t.) for the whole application (+ checking if a
	 * neighboring controller has at least one of its module , which would increase
	 * the d.t. by the "propagation delay" and "expected deploy time"). Also, the
	 * deployment time is both only bounded to the Application and provided as an
	 * instance field, which is NOT dynamic.</li>
	 * <li>node and module capacities (there's only a draft)</li>
	 * <li>ways to check if a module is / would be deployed in {fog ; neighboring ;
	 * control ; cloud } node and their respective distances</li>
	 * <li>the energy consumption for each modules (in the article, the addends are
	 * the energies of {fog nodes ("efn"); foc controller node ("efcn"); neighboring
	 * controller node ("enfcn"); cloud node ("ecn") }</li>
	 * </ul>
	 * 
	 * @param appsMapped given to provide context to the {@link AppModule} and the
	 *                   internal calculations
	 */
	public static double evaluateSolution(ChromosomeFog solution, Map<String, Application> appsMapped) {
		double evaluation, respTime, totalAccumulatedTime;
		AppModule m;
		FogDevice d;
		Application app;
		String appId;
		/**
		 * Store here the cumulative response time (got by accumulating the r.t. of each
		 * module of each application) in order to check if some deadline is NOT met
		 */
		Map<String, Double> cumulativeTimeConsuptionEachApp;

		cumulativeTimeConsuptionEachApp = new HashMap<>();
		evaluation = 0.0;
		for (PieceOfSolution pos : solution.getGenes()) {
			m = pos.module;
			d = pos.device;
			app = appsMapped.get(appId = m.getAppId());
			if (app != null) {
				totalAccumulatedTime = respTime = responseTime(m, d, app);
				if (cumulativeTimeConsuptionEachApp.containsKey(appId)) {
					cumulativeTimeConsuptionEachApp.put(appId,
							totalAccumulatedTime = respTime + cumulativeTimeConsuptionEachApp.get(appId));
				} else {
					cumulativeTimeConsuptionEachApp.put(appId, respTime);
				}
				if (app.getDeadlineMilliseconds() > 0 && totalAccumulatedTime > app.getDeadlineMilliseconds()) {
					return 0.0; // brutal interruption: constraint 2 not met
				}

				evaluation += respTime + energyConsumption(m, d, app);
			}
		}
		return evaluation;
	}

	public static double[][] resourcesMetricsPairs(AppModule m, FogDevice d) {
		PowerHost powerHost;
		powerHost = d.getHost();
		return new double[][] { //
				new double[] { // start power
						m.getMips() * d.getRatePerMips(), //
						d.getPower() //
				} // end power
				, new double[] { // start mips (million of instructions per seconds)
						m.getMips(), //
						powerHost.getAvailableMips() //
				} // end mips
				, new double[] { // start bandwidth
						m.getBw(), // TODO: getCurrentAllocatedBw() may be more suitable
						powerHost.getBw() //
				} // end bw
				, new double[] { // start ram
						m.getRam(), // TODO: getCurrentAllocatedRam() may be more suitable
						powerHost.getRam() //
				} // end ram
				, new double[] { // start "size" (TODO: very generic, undocumented: is it a local storage size?)
						m.getSize(), // TODO: probably, it will be changed to something more suitable
						powerHost.getStorage() //
				} // end size/storage
		};
	}

	public static double responseTime(AppModule module, FogDevice device, Application app) {
		boolean isInFog, isInNbr, isInController, isInCloud;
		double mkspanTime, executionTime, distFog, distNbr, distController, distCloud;
		DeviceNodeType dt;
		// note: all variables name are taken from the article

		mkspanTime = 0.0;
		executionTime = 0.0;
		isInFog = isInNbr = isInController = isInCloud = false;
		distFog = distNbr = distController = distCloud = 0.0;

		// TODO: how to get those values?

		// draft 1 : just adding up everything available

		for (double[] moduleNodeCapacity : resourcesMetricsPairs(module, device)) {
			if (moduleNodeCapacity[1] > 0.0) {
				executionTime += moduleNodeCapacity[0] / moduleNodeCapacity[1];
			}
		}

		mkspanTime = executionTime;

		dt = device.getDeviceNodeType();
		switch (dt) {
		case CloudNode:
			isInCloud = true;
			distCloud = device.getUplinkLatency();
			break;
		case FogControllerNode:
			isInController = true;
			distController = device.getUplinkLatency();
			break;
		case FogNode:
			isInFog = true;
			distFog = device.getUplinkLatency();
			break;
		case NeighboringFogControllerNode:
			isInNbr = true;
			distNbr = device.getUplinkLatency();
			break;
		default:
			break;
		}
		/*
		 * TODO : (FogDevice)CloudSim.getEntity(fogDeviceId) may be usefull, especially
		 * to crawl from the current device up to the cloud
		 */

		if (isInFog) {
			mkspanTime += distFog;
		}
		if (isInNbr) {
			mkspanTime += distNbr * 2.0;
		}
		if (isInController) {
			mkspanTime += distController;
		}
		if (isInCloud) {
			mkspanTime += distCloud * 2.0;
		}

		//

		return mkspanTime;
	}

	public static double energyConsumption(AppModule m, FogDevice d, Application app) {
		double energyNode, energyModule, efn, efcn, enfcn, ecn;
		PowerHost powerHost;

		energyModule = energyNode = 0.0;
		efn = efcn = enfcn = ecn = 0.0;

		powerHost = d.getHost();

		/*
		 * TODO: the article is a bit unclear about the exact formula; analyzing the
		 * PowerModel code the following code seems to fit the purpose:
		 */
		energyNode = powerHost.getPowerModel().getPower(
				// recycle "energyNode" variable as a temporary variable
				((energyNode = powerHost.getAvailableMips()) == 0.0 ? 0.0 : ( //
				m.getMips() / energyNode)));

		energyModule = efn + efcn + enfcn + ecn;

		return energyNode + energyModule;
	}

	/**
	 * Just calls {@link #newRandomSolution(Map, List, List, Random, ListDevices[])}
	 * by providing the result of {@link #partitionateDevicesByType(List)} as last
	 * parameter.
	 */
	public static SolutionModulesDeployed newRandomSolution(Map<String, Application> applicationsSubmitted,
			List<AppModule> modules, List<FogDevice> devices, Random r) {
		return newRandomSolution(applicationsSubmitted, modules, devices, r, partitionateDevicesByType(devices));
	}

	/**
	 * Generate a new random {@link PieceOfSolution} for a given {@link AppModule}
	 * that satisfy some design constraints, using one {@link FogDevice} from the
	 * provided ones
	 * <p>
	 * TODO: missing informations:
	 * <ul>
	 * <li>how to check if an application is delay tolerant</li>
	 * <li>AA</li>
	 * <li>AA</li>
	 * </ul>
	 * 
	 * @param applicationsSubmitted applications from which the modules comes from
	 * @param modules               the list of {@link AppModule} from where get the
	 *                              modules to base the solution
	 * @param devices               the list of {@link FogDevice} from which one has
	 *                              to be paired to the provided {@link AppModule}
	 * @param r                     just an instance of {@link Random}
	 * 
	 * @return a new randomly created solution
	 */

	public static SolutionModulesDeployed newRandomSolution(Map<String, Application> applicationsSubmitted,
			List<AppModule> modules, List<FogDevice> devices, Random r, final ListDevices[] devicesPartitions) {
		Application app;
		PieceOfSolution pieceOfSolution;
		List<PieceOfSolution> pieces;

		pieces = new ArrayList<>(modules.size());

		for (AppModule module : modules) {
			app = applicationsSubmitted.get(module.getAppId());
			if (app == null) {
				return null; // brutal interruption: constraint 1 can't be met
			}

			pieceOfSolution = newRandomPieceOfSolution(module, devicesPartitions, applicationsSubmitted, devices, r);
			if (pieceOfSolution == null) {
				return null; // brutal interruption: constraint 1 not met
			}

			pieces.add(pieceOfSolution);
		}
		return new SolutionModulesDeployed(pieces);
	}

	public static ListDevices[] partitionateDevicesByType(List<FogDevice> devices) {
		ListDevices[] devicesPartitions;
		DeviceNodeType[] types;
		types = DeviceNodeType.values();
		devicesPartitions = new ListDevices[types.length];
		for (DeviceNodeType dt : types) {
			devicesPartitions[dt.ordinal()] = new ListDevices();
		}
		devices.forEach(dd -> {
			devicesPartitions[dd.getDeviceNodeType().ordinal()].add(dd);
		});
		return devicesPartitions;
	}

	public static PieceOfSolution newRandomPieceOfSolution(AppModule module, final ListDevices[] devicesPartitions,
			Map<String, Application> applicationsSubmitted, List<FogDevice> devices, Random r) {
		boolean isOk;
		int iterationsLeft;
		ModuleType mt;
		List<FogDevice> listDevices;
		FogDevice d;
		Application app;

		mt = module.getModuleType();
		app = applicationsSubmitted.get(module.getAppId());
		if (app == null) {
			return null;
		}

		d = null;
		iterationsLeft = 10000;
		do {
			listDevices = devices;

			// knowing that physical devices can't go on cloud,
			// let's optimize by checking directly there where to retrieve the device
			if (mt == ModuleType.Physical) {
				int indexRnd, maxIter;
				maxIter = 10000;

				do {
					indexRnd = r.nextInt(devicesPartitions.length);
				} while ((indexRnd == DeviceNodeType.CloudNode.ordinal()
						|| (listDevices = devicesPartitions[indexRnd]).isEmpty()) && (--maxIter > 0));

				if (maxIter <= 0) {
					StringBuilder sb;
					String textError;
					sb = new StringBuilder(128);
					sb.append("No applicable device partition for module ");
					sb.append(module.getName());
					sb.append(" (app ID: ");
					sb.append(module.getAppId());
					sb.append(") even after 10000 tries.\nPartition: device type -- sizes:");
					for (DeviceNodeType dt : DeviceNodeType.values()) {
						sb.append("\n\t- ").append(dt.name()).append("\t - ")
								.append(devicesPartitions[dt.ordinal()].size());
					}
					textError = sb.toString();
					sb = null;
					throw new IllegalStateException(textError);
				}

			} // else : already initialized

			d = listDevices.get(r.nextInt(listDevices.size()));
			isOk = isAcceptableAssociation(module, d, app, devicesPartitions);
			if (!isOk) {
				d = null; // invalidate it
			}
		} while ((!isOk) && --iterationsLeft >= 0);

		if (d == null) {
			throw new IllegalStateException("No applicable device for module " + module.getName());
		}

		return new PieceOfSolution(module, d);
	}

	/**
	 * Returns {@code true} if the given {@link AppModule} can be assigned to the
	 * given {@link FogDevice} by checking the constraints. The {@link Application}
	 * instance is provided by means of context.
	 */
	public static boolean isAcceptableAssociation(AppModule module, FogDevice device, Application app,
			final ListDevices[] devicesPartitions) {
		double[][] resourcesMetricsPairs;
		ModuleType mt;

		mt = module.getModuleType();

		// constraint 4
		if (mt == ModuleType.Physical && device.getDeviceNodeType() == DeviceNodeType.CloudNode) {
			return false;
		}

		// constraint 3, somehow
		resourcesMetricsPairs = resourcesMetricsPairs(module, device);
		for (double[] pair : resourcesMetricsPairs) {
			if (pair[0] > pair[1]) {
				return false;
			}
		}

		return true;
	}

	//

	protected static class ListDevices extends ArrayList<FogDevice> {
		private static final long serialVersionUID = 1L;
	}
}