package org.network.builder;

import br.bons.core.OpticalNetworkProblem;
import br.cns.model.GmlData;
import br.cns.model.GmlNode;
import br.cns.persistence.GmlDao;
import cbic15.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import br.cns.model.GmlEdge;

import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExternalNetworkEvaluatorSettings extends AbstractIntegerProblem {

    private static final long serialVersionUID = 1L;
    private int lowerBound = 0;
    private int upperBound = 7;
    private GmlData gml;

    private List<Pattern>[] clustters;
    private Pattern[] centroids;
    private OpticalNetworkProblem opticalNetwork;
    private int contEvaluate = 0;
    IntegerSolution anterior;
    private List<String> fixedNetworkConections;
    private boolean FixedInitiallinks;
    private Properties prop;
    private boolean lineColumnChangedBefore = false;
    int gmlNumberRede = 1;
    private List<Integer[]> idCities;
    private List<Integer[]> arrayVar;
    private Map<Integer, Pattern> ClusterMap;
    private boolean IdidLoadFromVarX = false;
    private Map mapNode;


    /**
     * este m�todo � nativo da classe problem, ele ja recebe solution por
     * solution que vem de algum lugar com a cria��o inicial de forma
     * alet�ria. At� onde eu lembro ela n�o vem zerada na linnha da
     * invoca��o IntegerSolution retorno = new DefaultIntegerSolution(this); mas
     * se precisar modificar essa solutio "retorno" de alguma forma excepcional, o
     * lugar � neste m�todo. no nosso caso, ele usar o m�todo
     * alwaysTheSameSolution pra partir com os mesmos links sempre. obs: dependendo
     * do alg de cluster do pre processamento as cidades podem mudar, mas a linkagem
     * sempre virar de um arquivo var com o o nome de fixedSolution.tsv
     */

    @Override
    public IntegerSolution createSolution() {
        IntegerSolution retorno = new DefaultIntegerSolution(this);
        if (this.FixedInitiallinks) {
            retorno = this.alwaysTheSameSolution(retorno);
        }
        if (this.prop.getProperty("selectThePredeterminedPops").equals("y") && !this.lineColumnChangedBefore) {
            selectThePredeterminedPops();
        }
        if (this.prop.getProperty("startFromAstopedIteration").equals("y")) {
            if (this.prop.getProperty("fromGml").equals("y")) {
                retorno = this.retrievNetworksFromGmls();
                return retorno;
            } else {
                retorno = takeSolutionFromStopedExcution();
                return retorno;
            }
        }
        System.out.println("oh eu aqui de novo");
        retorno.setLineColumn(lineColumn.clone());
        return retorno;
    }

    /**
     * metodo escrito pra corrigir a posi��o dos centroids que retornam ordenados do
     * framwork do simtom
     *
     * @return
     */

    public Pattern[] locateTheCorrectPosition(Pattern[] linecollunm) {
        Pattern[] correctLineCollunm = new Pattern[this.clustters.length];
        for (int i = 0; i < linecollunm.length; i++) {
            for (int l = 0; l < this.clustters.length; l++) {
                for (Pattern p : this.clustters[l]) {
                    if (p.getId() == linecollunm[i].getId()) {
                        correctLineCollunm[l] = linecollunm[i];
                        break;
                    }
                }

            }
        }
        return correctLineCollunm;

    }

    /**
     * esse metodo foi escrito para recuperar redes de uma execucao parada a partir
     * de arquivos gml porem como a lista de cidades eh ordenada na hora de salvar o
     * gml, ao ser recuperada ela perde a ordem original o que desemparelha ela como
     * o arrey de lista clusteres. isso atrapalhou a logica da busca que consideram
     * o emparelhamento como a seguran�a de que a cidade indice 1 do lineCollumn
     * pertence cluster 1 no array de lista de cluster.
     *
     * @return
     */

    public IntegerSolution retrievNetworksFromGmls() {
        OpticalNetworkProblem p = new OpticalNetworkProblem();
        GmlDao gml = new GmlDao();
        String path = this.prop.getProperty("pathStartStopedExecution") + "/" + "execu��o "
                + this.prop.getProperty("executionStoped") + "/" + "ResultadoGML"
                + this.prop.getProperty("interationStopedInExecution") + "/" + Integer.toString(this.gmlNumberRede)
                + ".gml";
        GmlData gmlData = gml.loadGmlData(path);
        // o gmlData original estava trocando os ids originais
        // estao esse foi criado so pra recuperar os ids corretos
        GmlData gmlDataOfCorrectIndex = gml.loadGmlDataWithTheSamId(path, true);
        p.reloadProblem(Integer.parseInt(this.prop.getProperty("erlangs")), gmlData);
        IntegerSolution solution = new DefaultIntegerSolution(this);
        Pattern[] lineCollunm = new Pattern[this.clustters.length];
        lineCollunm = this.ptg.takePatternData(gmlDataOfCorrectIndex);
        lineCollunm = locateTheCorrectPosition(lineCollunm.clone());
        solution.setLineColumn(lineCollunm);
        if (!this.IdidLoadFromVarX) {
            try {
                path = this.prop.getProperty("pathStartStopedExecution") + "/" + "execu��o "
                        + this.prop.getProperty("executionStoped") + "/" + "var" + this.prop.getProperty("interationStopedInExecution") + ".tsv";
                getTheChromosomeFronVarX(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Integer[] ArrayVar = this.arrayVar.get(0);
        this.arrayVar.remove(0);

        for (int i = 0; i < ArrayVar.length; i++) {
            solution.setVariableValue(i, ArrayVar[i]);
        }

//		Integer[] variables = p.getDefaultSolution();
//		for (int i = 0; i < this.getNumberOfVariables(); i++) {
//			solution.setVariableValue(i, variables[i]);
//
//		}

        this.gmlNumberRede += 1;
        return solution;
    }

    /**
     * metodo l� as conex�es da polu��o iniciala partir de um array list no
     * atributos Observe que ele s� pode fazer isso uma vez j� que ele remove a
     * cabe�a da lista fixedNetworkConections, que guarda a lista de strings
     * recuperada de um arquivo var.tsv pelo m�todo
     * retrieveTheFixedInitialNetworks. Uu seja, apos carregar a popula��o
     * inicial a lista fixedNetworkConections ser� esvaziada.
     *
     * @param s
     * @return
     */

    public IntegerSolution alwaysTheSameSolution(IntegerSolution s) {

        String[] rede = new String[this.getNumberOfVariables()];
        rede = this.fixedNetworkConections.get(0).split(" ");

        for (int i = 0; i < rede.length; i++) {
            s.setVariableValue(i, Integer.parseInt(rede[i]));
        }
        this.fixedNetworkConections.remove(0);
        return s;
    }

    /**
     * esse dois metodos a seguir são o polimorfismo do metodo que coloca um
     * conjunto de pops predeterminado pelo usuário, não mais como uma decisão do
     * alg.
     *
     * @param s
     * @return
     */

    /*
     * public IntegerSolution selectThePredeterminedPops(IntegerSolution s) {
     * Pattern[] lineColumnLocal = new Pattern[this.kmeans.getK()]; String
     * presetctedPop = this.prop.getProperty("hardPop"); String[] arrayIdPop =
     * presetctedPop.split(","); for (int i = 0; i < this.clustters.length; i++) {
     *
     * for (int w = 0; w < arrayIdPop.length; w++) {
     *
     * for (Pattern p : this.clustters[i]) { if
     * (p.getId()==Integer.parseInt(arrayIdPop[w])) { lineColumnLocal[w]=p; } } } }
     *
     * s.setLineColumn(lineColumnLocal); return s; }
     */
    public void selectThePredeterminedPops() {
        Pattern[] lineColumnLocal = new Pattern[this.kmeans.getK()];
        String presetctedPop = this.prop.getProperty("hardPop");
        String[] arrayIdPop = presetctedPop.split(";");
        for (int i = 0; i < this.clustters.length; i++) {

            for (int w = 0; w < arrayIdPop.length; w++) {

                for (Pattern p : this.clustters[i]) {
                    if (p.getId() == Integer.parseInt(arrayIdPop[w])) {
                        lineColumnLocal[w] = p;
                    }
                }
            }
        }
        this.lineColumn = lineColumnLocal;
        this.lineColumnChangedBefore = true;
        SetNetWork();
    }

    public void retrieveTheFixedInitialNetworks() throws IOException {

        BufferedReader br;
        try {

            br = new BufferedReader(new FileReader(this.prop.getProperty("pathS.U")));
            String linha;
            List<String> lista = new ArrayList();
            while ((linha = br.readLine()) != null) {
                lista.add(linha);

            }
            this.fixedNetworkConections = lista;
        } catch (Exception e) {
            String path = this.prop.getProperty("local") + this.prop.getProperty("pathS.U");
            path = path.replace("\\resultados", "");
            br = new BufferedReader(new FileReader(path));
            String linha;
            List<String> lista = new ArrayList();
            while ((linha = br.readLine()) != null) {
                lista.add(linha);

            }
            this.fixedNetworkConections = lista;
        }


    }

    /**
     * construcao de um hashmap dos cluster pra encontrar um pattern sem muito custo
     * sera usado no metodo que levanta as solucoes a partir do print.txt e do
     * var.tsv
     */
    public void constructMapNodeClusters() {
        List<Pattern[]> listLineCollumn = new ArrayList<>();
        Map<Integer, Pattern> ClusterMap = new HashMap<Integer, Pattern>();

        for (int l = 0; l < this.clustters.length; l++) {
            for (Pattern p : this.clustters[l]) {
                ClusterMap.put(p.getId(), p);
            }

        }
        this.ClusterMap = ClusterMap;
    }

    /**
     * constroi uma lista de array de ids de cidades a partir do arquivo print.txt
     * para ser usado na recosntrucao das solutions a partir de uma execucao parada
     *
     * @throws IOException
     */

    public void constructArrayIdCities() throws IOException {
        if (!(this.prop.getProperty("fromGml").equals("y"))) {
            BufferedReader br = new BufferedReader(new FileReader(this.prop.getProperty("pathStartStopedExecution")
                    + "\\" + "execu��o " + this.prop.getProperty("executionStoped") + "/" + "print.txt"));
            String linha;
            List<String> idCities = new ArrayList();
            while ((linha = br.readLine()) != null) {
                String[] arrayLinha = linha.split(" ");
                if (arrayLinha[0].equals("centroides") && arrayLinha[1].equals("final")) {
                    linha = linha.replace("centroides final : ", "");
                    linha = linha.replace("[", "");
                    linha = linha.replace("]", "");
                    linha = linha.replace(",", "");
                    idCities.add(linha);
                }
            }
            List<Integer[]> arrayId = new ArrayList<>();
            for (String s : idCities) {
                String[] l = s.split(" ");
                Integer[] intInCities = new Integer[l.length];
                for (int i = 0; i < l.length; i++) {
                    intInCities[i] = Integer.parseInt(l[i]);
                }
                arrayId.add(intInCities);
            }
            this.idCities = arrayId;

        }

    }

    /**
     * constroi uma lista de array de variable a partir do arquivo var.tsv para ser
     * usado na recosntrucao das solutions a partir de uma execucao parada
     * com uma  falha por excecao
     *
     * @throws IOException
     */
    public void constructArrayChromosome() throws IOException {

        if (!(this.prop.getProperty("fromGml").equals("y"))) {
            BufferedReader br = new BufferedReader(new FileReader(this.prop.getProperty("pathStartStopedExecution")
                    + "\\" + "execu��o " + this.prop.getProperty("executionStoped") + "/" + "var.tsv"));
            String linha;
            List<String> variableString = new ArrayList();
            while ((linha = br.readLine()) != null) {
                String[] arrayLinha = linha.split(" ");
                variableString.add(linha);
            }
            List<Integer[]> arrayvar = new ArrayList<>();
            for (String s : variableString) {
                String[] l = s.split(" ");
                Integer[] intVar = new Integer[l.length];
                for (int i = 0; i < l.length; i++) {
                    intVar[i] = Integer.parseInt(l[i]);
                }
                arrayvar.add(intVar);
            }
            this.arrayVar = arrayvar;
        }

    }

    /**
     * esse medodo e um auxiliar para a tecnica que recomeca a partir de um arquivo gml.
     * Constroi uma lista de array de variable a partir do arquivo varX.tsv para ser
     * usado na recosntrucao das solutions a partir de uma execucao parada porem
     * parada sem falha por excecao
     *
     * @throws IOException
     */
    public void getTheChromosomeFronVarX(String path) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(path));
        String linha;
        List<String> variableString = new ArrayList();
        while ((linha = br.readLine()) != null) {
            String[] arrayLinha = linha.split(" ");
            variableString.add(linha);
        }
        List<Integer[]> arrayvar = new ArrayList<>();
        for (String s : variableString) {
            String[] l = s.split(" ");
            Integer[] intVar = new Integer[l.length];
            for (int i = 0; i < l.length; i++) {
                intVar[i] = Integer.parseInt(l[i]);
            }
            arrayvar.add(intVar);
        }
        this.IdidLoadFromVarX = true;
        this.arrayVar = arrayvar;

    }


    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        int load = 100;
        Integer[] vars = new Integer[solution.variables().size()];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = (Integer) solution.variables().get(i);
        }

        System.out.println("conte Evaluate: " + this.contEvaluate);
        this.contEvaluate += 1;
        GmlData gmlData = getGmlData(gml.getNodes(), vars);
        if (gmlData.containsIsolatedNodes()) {
            solution.objectives()[0]=1.000000;
            solution.objectives()[1]= 100000.00;
            solution.objectives()[2]= 100000000.00;
            solution.objectives()[3]= 1;
        } else {
            OpticalNetworkProblem P = new OpticalNetworkProblem();
            P.reloadProblem(load, gmlData);
            Double[] objectives = P.evaluate(vars);
            solution.objectives()[0]= objectives[0];
            solution.objectives()[1]= objectives[1];
            solution.objectives()[2]= objectives[2];
            solution.objectives()[3]= 1 / (1 + objectives[3]);
        }
        return solution;
    }


    public GmlData getGmlData(List<GmlNode> nodes, Integer[] vars) {
        GmlDao gmlDao = new GmlDao();
        GmlData gmlData = new GmlData();
        gmlData.setNodes(nodes);
        var links = buildLink(nodes, vars);
        gmlData.setEdges(links);
        gmlData.createComplexNetwork();
        gmlData = gmlDao.loadGmlDataFromContent(gmlDao.createFileContent(gmlData));
        return gmlData;
    }


    public List<GmlEdge> buildLink(List <GmlNode> nodesList, Integer[] vars) {
        //it is used to allows modification inside lambda
        int[] varIndex = {0};
        List<GmlEdge> edges = new ArrayList<>();
        List<Integer> varList = Arrays.asList(vars);

        return IntStream.range(0, nodesList.size())
                .boxed()
                .flatMap(i -> IntStream.range(i, nodesList.size())
                        .filter(j -> i != j)
                        .mapToObj(j -> {
                            GmlEdge edge = null;
                            if (varList.get(varIndex[0]) == 1) {
                                edge = new GmlEdge();
                                edge.setSource((GmlNode) this.mapNode.get(nodesList.get(i).getId()));
                                edge.setTarget((GmlNode) this.mapNode.get(nodesList.get(j).getId()));
                                edges.add(edge);
                            }
                            varIndex[0] += 3;
                            return edge;
                        })
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }


    public List<GmlNode> patternGml(Pattern[] ArrayPatterns) {
        List<GmlNode> listNode = new ArrayList<>();
        for (int i = 0; i < ArrayPatterns.length; i++) {
            Integer id = ArrayPatterns[i].getId();
            listNode.add((GmlNode) this.mapNode.get(ArrayPatterns[i].getId()));
        }
        return listNode;
    }

    // @Override
    // public int getNumberOfVariables() {
    //
    // return this.kmeans.getCentroids().length
    // *((this.kmeans.getCentroids().length-1)/2);
    // }





    public void SetNetWork() {
        int load = Integer.parseInt(this.prop.getProperty("erlangs"));
        Integer[] vars = new Integer[this.getNumberOfVariables()];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = 1;
        }
        this.ptg.patternGmlData(this.lineColumn, vars);
        String path = "src/Gmlevaluating.gml";

        try {
            this.opticalNetwork = new OpticalNetworkProblem(load, path);
        } catch (Exception e) {
            path = this.prop.getProperty("local");
            path = path.replace("\\resultados", "");
            path += "src/Gmlevaluating.gml";
            this.opticalNetwork = new OpticalNetworkProblem(load, path);
        }
    }

    public GmlData getGml() {
        return gml;
    }




    private void gmlBuild() {

        GmlData gml = null;
        String path = "./gml";
        try {

            this.gml = new GmlDao().loadGmlData(path);

        } catch (Exception e) {
            path = path.replace("src", "/RedeParaCECin/src/");
            FileSystemView system = FileSystemView.getFileSystemView();
            path = system.getHomeDirectory().getPath() + path;
            this.gml = new GmlDao().loadGmlData(path);
        }

        gml.getNodes();

        setGmlMap(gml);
    }


    private void setGmlMap(GmlData gml) {
        Map<Integer, GmlNode> map = new HashMap<Integer, GmlNode>();
        for (GmlNode gmlNode : gml.getNodes()) {
            map.put(gmlNode.getId(), gmlNode);
        }
        this.mapNode = map;
    }


    public ExternalNetworkEvaluatorSettings(@JsonProperty("prop") Properties prop) {
        super();
        gmlBuild();
        this.numberOfObjectives(2);
        var numberOfEdges = gml.getEdges().size();
        var numberOfVariables = (3 * numberOfEdges * (numberOfEdges - 1) / 2 + numberOfEdges + 1);

        /**problem configuration */
        List<Integer> ll = new Vector<>();
        List<Integer> ul = new Vector<>();
        var roadmPart = numberOfEdges + 1;
        var matrixConnectionPart = numberOfVariables - roadmPart;

        for (int i = 0; i < matrixConnectionPart; i++) {
            ll.add(0);
            ul.add(7);
        }

        for (int i = numberOfVariables - roadmPart; i < numberOfVariables; i++) {
            if (i < numberOfVariables - 1) {
                ll.add(1);
                ul.add(12);
            } else {
                ll.add(4);
                ul.add(40);
            }
        }
        this.variableBounds(ll, ul);
        /** end problem configuration*/

        this.prop = prop;
        try {
            retrieveTheFixedInitialNetworks();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (/* fixedLinks.equals("y") */prop.getProperty("solucaoInicialUnica").equals("y")) {
            this.FixedInitiallinks = true;
        } else {
            this.FixedInitiallinks = false;
        }

        SetNetWork();
        printIncialCentroide();
        if (prop.getProperty("startFromAstopedIteration").equals("y")) {
            this.constructMapNodeClusters();
            try {
                this.constructArrayIdCities();
                this.constructArrayChromosome();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


}
