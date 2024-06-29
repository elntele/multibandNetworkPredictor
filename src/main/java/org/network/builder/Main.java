package org.network.builder;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Problem<IntegerSolution> problem; // do Jmetal
        Algorithm<List<IntegerSolution>> algorithm; // do Jmetal
        CrossoverOperator<IntegerSolution> crossover; // do Jmetal
        MutationOperator<IntegerSolution> mutation; // do Jmetal
        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection; // do
        problem = new ExternalNetworkEvaluatorSettings(kmeans, gml, clustters,
                /* prop.get("solucaoInicialUnica").toString() */ prop);




        // ****************************
        double crossoverProbability = 1.0;
        double crossoverDistributionIndex = 20.0;
        crossover = new IntegerSBXCrossover(crossoverProbability, crossoverDistributionIndex);
        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        mutation = new IntegerPolynomialMutation(mutationProbability, mutationDistributionIndex);
        selection = new BinaryTournamentSelection<IntegerSolution>();

         algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, 100) .setSelectionOperator(selection).setMaxEvaluations(1000).build();
         AlgorithmRunner algorithmRunner = new  AlgorithmRunner.Executor(algorithm).execute();
        List<IntegerSolution> population;
        population = algorithm.result();
        int w = 1;

        String path = prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo") + "/"
                + prop.getProperty("execucao") + "/" + "print.txt";

        FileWriter arq = new FileWriter(path);
        PrintWriter gravarArq = new PrintWriter(arq);

        PatternToGml ptgLocal = ((ExternalNetworkEvaluatorSettings) problem).getPtg();
        new File(prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo") + "/"
                + prop.getProperty("execucao") + "/ResultadoGML").mkdir();
        String pathTogml = prop.getProperty("local") + prop.getProperty("algName") + "/" + prop.getProperty("modo")
                + "/" + prop.getProperty("execucao");
        for (IntegerSolution i : population) {
            String s = pathTogml + "/ResultadoGML/" + Integer.toString(w) + ".gml";
            ptgLocal.saveGmlFromSolution(s, i);
            List<Integer> centros = new ArrayList<>();
            for (int j = 0; j < i.getLineColumn().length; j++) {
                centros.add(i.getLineColumn()[j].getId());
            }
            w += 1;
            System.out.println("centroides final : " + centros);
            gravarArq.printf("centroides final : " + centros + '\n');

        }

        if (prop.get("parallelFitness").equals("y")){
            int fit= ((ExternalNetworkEvaluatorSettings) problem).getContEvaluate()+parallelEvaluator.getAuxiliarCountParallelEvaluation();
            System.out
                    .println("numero de avalia��es de fitness " + (fit));
            gravarArq.printf(
                    "numero de avalia��es de fitness" + ((ExternalNetworkEvaluatorSettings) problem).getContEvaluate() + '\n');
            System.out.println("base salva em formato GML");
            System.out.println("numero de solu��es n�o dominadas encontrado pela busca local: "
                    + ((NSGAIII) algorithm).getLocalSeachFoundNoDominated());
            gravarArq.printf("numero de solu��es n�o dominadas encontrado pela busca local: "
                    + ((NSGAIII) algorithm).getLocalSeachFoundNoDominated() + '\n');
            long computingTime = algorithmRunner.getComputingTime();
            JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
            gravarArq.printf("Total execution time: " + computingTime + "ms" + '\n');
            List<List<Integer>> IndexOfSelectionedToTheSearche=((NSGAIII) algorithm).getIndexOfIndividualSelectionedToTheSearch();
            List<List<Double>> equalizadeList =((NSGAIII) algorithm).getEqualizadListe();
            printFinalSolutionSet(population, kmeans, arq, gravarArq, prop,IndexOfSelectionedToTheSearche,equalizadeList);
        }else {
            System.out
                    .println("numero de avalia��es de fitness" + ((ExternalNetworkEvaluatorSettings) problem).getContEvaluate());
            gravarArq.printf(
                    "numero de avalia��es de fitness" + ((ExternalNetworkEvaluatorSettings) problem).getContEvaluate() + '\n');
            System.out.println("base salva em formato GML");
            System.out.println("numero de solu��es n�o dominadas encontrado pela busca local: "
                    + ((NSGAIII) algorithm).getLocalSeachFoundNoDominated());
            gravarArq.printf("numero de solu��es n�o dominadas encontrado pela busca local: "
                    + ((NSGAIII) algorithm).getLocalSeachFoundNoDominated() + '\n');
            long computingTime = algorithmRunner.getComputingTime();
            JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
            gravarArq.printf("Total execution time: " + computingTime + "ms" + '\n');
            List<List<Integer>> IndexOfSelectionedToTheSearche=((NSGAIII) algorithm).getIndexOfIndividualSelectionedToTheSearch();
            List<List<Double>> equalizadeList =((NSGAIII) algorithm).getEqualizadListe();
            printFinalSolutionSet(population, kmeans, arq, gravarArq, prop,IndexOfSelectionedToTheSearche,equalizadeList);

        }
}