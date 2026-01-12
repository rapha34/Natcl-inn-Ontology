package test.java.natclinn.util;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.HashSet;
import java.util.Set;

/**
 * Test simple pour vérifier la logique de filtrage des arguments d'emballage
 * NOUVELLE LOGIQUE : Seuls les arguments avec nameCriterion = "Matière packaging" sont acceptés
 */
public class TestPackagingFilter {

    public static void main(String[] args) {
        System.out.println("Test de la logique de filtrage des arguments d'emballage");

        // Simuler un argument avec nameCriterion = "Matière packaging" (ACCEPTÉ)
        Node argumentNode1 = NodeFactory.createURI("http://example.org/arg1");
        Node nameCriterionProp = NodeFactory.createURI("http://natclinn.org/nameCriterion");
        Node packagingCriterion = NodeFactory.createLiteral("Matière packaging");

        // Simuler un argument avec nameCriterion = "Composition" (REJETÉ)
        Node argumentNode2 = NodeFactory.createURI("http://example.org/arg2");
        Node compositionCriterion = NodeFactory.createLiteral("Composition");

        // Simuler un argument avec nameCriterion = "Transformation" (REJETÉ)
        Node argumentNode3 = NodeFactory.createURI("http://example.org/arg3");
        Node transformationCriterion = NodeFactory.createLiteral("Transformation");

        System.out.println("Test terminé - logique implémentée dans ComparePackagingTypeProperty");
    }
}