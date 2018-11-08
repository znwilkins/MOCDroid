package mainPackage;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Evaluator {

    private static final int BENIGNWARE = 0;
    private static final int MALWARE = 1;

    private int numClustersMalware;
    private int numClustersBenignware;

    private HashMap<Integer, ArrayList<String>> clustersMalware;
    private HashMap<Integer, ArrayList<String>> clustersBenignware;

    private int[][] exampleClusterNumImportsTESTmalware;
    private int[][] exampleClusterNumImportsTESTbenignware;

    private ArrayList<Example> testingSamples = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String malwareClusterFilePath = args[0];
        String benignClusterFilePath = args[1];
        String testingImportsFilePath = args[2];
        String serializedDetectorsDirectory = args[3];

        Evaluator evaluator = new Evaluator();
        evaluator.init(malwareClusterFilePath, benignClusterFilePath, testingImportsFilePath);

        List<int[]> individuals =
                Files
                    .list(Paths.get(serializedDetectorsDirectory))
                    .sorted()  // Not strictly necessary, maintains ordering between runs
                    .map(p -> {
                        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(p.toFile()))) {
                            return (int[]) ois.readObject();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

        evaluator.processSolutions(individuals);
    }

    private void init(String malwareClusterFilePath, String benignClusterFilePath, String testingImportsFilePath) throws Exception {
        clustersMalware = ReadClusters.readFile(malwareClusterFilePath);
        clustersBenignware = ReadClusters.readFile(benignClusterFilePath);

        numClustersMalware = clustersMalware.size();
        numClustersBenignware = clustersBenignware.size();

        for (String sampleImports : Files.readAllLines(Paths.get(testingImportsFilePath), StandardCharsets.UTF_8)) {
            testingSamples.add(new Example(MALWARE, sampleImports.split(" ")));
        }

        exampleClusterNumImportsTESTmalware = new int[testingSamples.size()][];
        exampleClusterNumImportsTESTbenignware = new int[testingSamples.size()][];

        for (int i = 0; i < testingSamples.size(); i++) {
            String[] importsArray = testingSamples.get(i).getImportsArray();

            exampleClusterNumImportsTESTbenignware[i] = getNumImportsByCluster(importsArray, clustersBenignware);
            exampleClusterNumImportsTESTmalware[i] = getNumImportsByCluster(importsArray, clustersMalware);
        }

    }

    private void processSolutions(List<int[]> individuals) {
        // For every detector in the pareto frontier...
        for (int[] genome : individuals){

            int [] genomeMalware = new int[numClustersMalware];
            int [] genomeBenignware = new int[numClustersBenignware];
            System.arraycopy(genome, 0, genomeMalware, 0, numClustersMalware);
            System.arraycopy(genome, numClustersMalware, genomeBenignware, 0, numClustersBenignware);

            int individualScore = 0;
            int individualFalsePositives = 0;
            int individualFalseNegatives = 0;
            int individualTruePositives = 0;
            int individualTrueNegatives = 0;

            for (int numExample = 0; numExample < testingSamples.size(); numExample++){
                Example example = testingSamples.get(numExample);

                double scoreClusteringMalware = getScoreClusteringModelFAST(genomeMalware, exampleClusterNumImportsTESTmalware, clustersMalware, numExample);
                double scoreClusteringBenignware = getScoreClusteringModelFAST(genomeBenignware, exampleClusterNumImportsTESTbenignware, clustersBenignware, numExample);

                int labelAssigned = scoreClusteringMalware > scoreClusteringBenignware ? MALWARE : BENIGNWARE;

                if (labelAssigned == example.getLabel()){
                    individualScore +=1;
                }

                if (labelAssigned == MALWARE && example.getLabel() == BENIGNWARE){
                    individualFalsePositives += 1;
                }
                if (labelAssigned == MALWARE && example.getLabel() == MALWARE){
                    individualTruePositives += 1;
                }
                if (labelAssigned == BENIGNWARE && example.getLabel() == BENIGNWARE){
                    individualTrueNegatives += 1;
                }
                if (labelAssigned == BENIGNWARE && example.getLabel() == MALWARE){
                    individualFalseNegatives += 1;
                }

            }

            double accuracy = (double) individualScore / (double) testingSamples.size();

            System.out.println("Testing accuracy: " + accuracy);
            System.out.println("True positives: " + individualTruePositives);
            System.out.println("False negatives: " + individualFalseNegatives);
            System.out.println("False positives: " + individualFalsePositives);
            System.out.println("True negatives: " + individualTrueNegatives);
            System.out.println("----------------\n");

        }
    }

    private static double getScoreClusteringModelFAST(int[] genome, int[][] clusteringExamplesRepresentations, HashMap<Integer, ArrayList<String>> clusteringModel, int numExample) {
        int score = 0;
        for (int numCluster = 0; numCluster < genome.length; numCluster++){
            if (genome[numCluster] != 1){
                continue;
            }

            int numImportsClusterRepresented = clusteringExamplesRepresentations[numExample][numCluster];

            if (numImportsClusterRepresented == clusteringModel.get(numCluster).size()){
                score += 1;
            }
        }
        return (double) score / (double) genome.length;
    }

    private static int[] getNumImportsByCluster(String [] example, HashMap<Integer, ArrayList<String>> clusteringModel) {
        int[] numImportsByCluster = new int[clusteringModel.size()];

        Collection<String> listExample = new ArrayList<>(Arrays.asList(example));

        for (int numCluster = 0; numCluster < clusteringModel.size(); numCluster++) {

            Collection<String> listCluster = new ArrayList<>(clusteringModel.get(numCluster));

            Collection<String> listExampleAux = new ArrayList<>(listExample);
            listExampleAux.retainAll(listCluster);
            int countImportsRepresented = listExampleAux.size();

            numImportsByCluster[numCluster] = countImportsRepresented;
        }

        return numImportsByCluster;
    }

}
