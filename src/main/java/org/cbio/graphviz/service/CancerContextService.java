package org.cbio.graphviz.service;

import flexjson.JSONSerializer;
import org.cbio.graphviz.model.*;
import org.cbio.graphviz.util.StudyFileUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.*;

/**
 * Service to retrieve predefined study data as a graph.
 *
 * @author Selcuk Onur Sumer
 */
public class CancerContextService
{
	// source directory for the edge list files
	private Resource edgeListResource;

	public Resource getEdgeListResource() {
		return edgeListResource;
	}

	public void setEdgeListResource(Resource edgeListResource) {
		this.edgeListResource = edgeListResource;
	}

	@Cacheable("cancerContextStudiesCache")
	public String listAvailableCancers() throws IOException
	{
		JSONSerializer jsonSerializer = new JSONSerializer().exclude("*.class");

		File edgeListDir = this.getEdgeListResource().getFile();
		List<CancerStudy> cancerStudies = new ArrayList<>();
		Map<String, String> id2NameMap = studyId2NameMap();
		Set<String> ids = new HashSet<>();

		if (edgeListDir.isDirectory())
		{
			for (File edgeList: edgeListDir.listFiles())
			{
				String[] parts = edgeList.getName().split("_");

				if (parts.length > 1)
				{
					ids.add(parts[1]);
				}
			}

			for (String id: ids)
			{
				CancerStudy study = new CancerStudy();

				study.setStudyId(id);
				study.setStudyName(id2NameMap.get(id));

				cancerStudies.add(study);
			}
		}

		return jsonSerializer.deepSerialize(cancerStudies);
	}

	private Map<String, String> studyId2NameMap()
	{
		Map<String, String> id2NameMap = new HashMap<>();

		id2NameMap.put("BLCA", "Bladder Urothelial Carcinoma (BLCA)");
		id2NameMap.put("BRCA", "Breast Invasive Carcinoma (BRCA)");
		id2NameMap.put("COAD", "Colon Adenocarcinoma (COAD)");
		id2NameMap.put("GBM", "Glioblastoma Multiforme (GBM)");
		id2NameMap.put("HNSC", "Head And Neck Squamous Cell Carcinoma (HNSC)");
		id2NameMap.put("KIRC", "Kidney Renal Clear Cell Carcinoma (KIRC)");
		id2NameMap.put("LUAD", "Lung Adenocarcinoma (LUAD)");
		id2NameMap.put("LUSC", "Lung Squamous Cell Carcinoma (LUSC)");
		id2NameMap.put("OV", "Ovarian Serous Cystadenocarcinoma (OVCA)");
		id2NameMap.put("READ", "Rectum Adenocarcinoma (READ)");
		id2NameMap.put("UCEC", "Uterine Corpus Endometrioid Carcinoma (UCEC)");

		return id2NameMap;
	}

	@Cacheable("cancerContextMethodsCache")
	public String listAvailableMethods() throws IOException
	{
		JSONSerializer jsonSerializer = new JSONSerializer().exclude("*.class");

		File edgeListDir = this.getEdgeListResource().getFile();
		List<Method> methods = new ArrayList<>();
		Set<String> methodNames = new HashSet<>();

		if (edgeListDir.isDirectory())
		{
			for (File edgeList: edgeListDir.listFiles())
			{
				String[] parts = edgeList.getName().split("_");
				String method = "";

				for (int i = 2; i < parts.length; i++)
				{
					if (i == parts.length - 1)
					{
						method += parts[i].substring(0, parts[i].indexOf("."));
					}
					else
					{
						method += parts[i];
						method += "_";
					}
				}

				methodNames.add(method);
			}

			for (String name: methodNames)
			{
				Method m = new Method();

				m.setMethodId(name);
				m.setMethodName(name.substring(0,1).toUpperCase() + name.substring(1));

				methods.add(m);
			}
		}

		return jsonSerializer.deepSerialize(methods);
	}

	@Cacheable("cancerContextDataCache")
	public String getStudyData(String study,
			String method,
			Integer size) throws IOException
	{
		JSONSerializer jsonSerializer = new JSONSerializer().exclude("*.class");

		CytoscapeJsGraph graph = new CytoscapeJsGraph();
		List<CytoscapeJsEdge> edges = this.getEdgeList(study, method, size);
		List<CytoscapeJsNode> nodes = this.getNodeList(edges);

		graph.setEdges(edges);
		graph.setNodes(nodes);

		return jsonSerializer.deepSerialize(graph);
	}

	protected List<CytoscapeJsNode> getNodeList(List<CytoscapeJsEdge> edges)
	{
		List<CytoscapeJsNode> nodes = new ArrayList<>();
		Map<Object, CytoscapeJsNode> map = new HashMap<>();

		for (CytoscapeJsEdge edge: edges)
		{
			Object prot1 = edge.getProperty(PropertyKey.PROT1);
			Object prot2 = edge.getProperty(PropertyKey.PROT2);

			Object gene1 = edge.getProperty(PropertyKey.GENE1);
			Object gene2 = edge.getProperty(PropertyKey.GENE2);

			if (map.get(prot1) == null)
			{
				CytoscapeJsNode node = new CytoscapeJsNode();

				node.setProperty(PropertyKey.ID, prot1);
				node.setProperty(PropertyKey.PROT, prot1);
				node.setProperty(PropertyKey.GENE, gene1);

				map.put(prot1, node);
				nodes.add(node);
			}

			if (map.get(prot2) == null)
			{
				CytoscapeJsNode node = new CytoscapeJsNode();

				node.setProperty(PropertyKey.ID, prot2);
				node.setProperty(PropertyKey.PROT, prot2);
				node.setProperty(PropertyKey.GENE, gene2);

				map.put(prot2, node);
				nodes.add(node);
			}
		}

		return nodes;
	}

	protected List<CytoscapeJsEdge> getEdgeList(String study,
			String method,
			Integer size) throws IOException
	{
		String filename = this.getEdgeListResource().getFile().getAbsolutePath() +
				"/edgelist_" + study + "_" + method + ".txt";

		BufferedReader in = new BufferedReader(new FileReader(filename));

		// assuming the file is not empty & first line is the header
		String headerLine = in.readLine();

		List<CytoscapeJsEdge> edges = new ArrayList<>();

		StudyFileUtil util = new StudyFileUtil(headerLine);
		String line;

		for (int i = 0;
		     i < size && (line = in.readLine()) != null;
		     i++)
		{
			edges.add(util.parseLine(line));
		}

		in.close();

		return edges;
	}
}
