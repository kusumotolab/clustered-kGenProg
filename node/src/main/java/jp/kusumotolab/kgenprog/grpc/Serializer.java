package jp.kusumotolab.kgenprog.grpc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import jp.kusumotolab.kgenprog.Configuration;
import jp.kusumotolab.kgenprog.ga.Base;
import jp.kusumotolab.kgenprog.ga.Gene;
import jp.kusumotolab.kgenprog.project.ASTLocation;
import jp.kusumotolab.kgenprog.project.FullyQualifiedName;
import jp.kusumotolab.kgenprog.project.Operation;
import jp.kusumotolab.kgenprog.project.ProductSourcePath;
import jp.kusumotolab.kgenprog.project.TargetFullyQualifiedName;
import jp.kusumotolab.kgenprog.project.TestFullyQualifiedName;
import jp.kusumotolab.kgenprog.project.build.BuildResults;
import jp.kusumotolab.kgenprog.project.build.JavaBinaryObject;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.jdt.DeleteOperation;
import jp.kusumotolab.kgenprog.project.jdt.InsertOperation;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTConstruction;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTLocation;
import jp.kusumotolab.kgenprog.project.jdt.ReplaceOperation;
import jp.kusumotolab.kgenprog.project.test.Coverage;
import jp.kusumotolab.kgenprog.project.test.EmptyTestResults;
import jp.kusumotolab.kgenprog.project.test.TestResult;
import jp.kusumotolab.kgenprog.project.test.TestResults;


/**
 * KGenProgクラスとgRPCクラスの相互変換を行うメソッド群 
 * serialize: KGenProg -> gRPC 
 * deserialize: gRPC -> KGenProg
 */
public final class Serializer {

  private Serializer() {}

  public static GrpcConfiguration serialize(
      final Configuration configuration) {
    final TargetProject project = configuration.getTargetProject();

    final GrpcConfiguration.Builder builder = GrpcConfiguration.newBuilder()
        .setRootDir(project.rootPath.toString());

    project.getProductSourcePaths()
        .stream()
        .map(p -> p.path.toString())
        .forEach(builder::addProductPaths);

    project.getTestSourcePaths()
        .stream()
        .map(p -> p.path.toString())
        .forEach(builder::addTestPaths);

    project.getClassPaths()
        .stream()
        .map(p -> p.path.toString())
        .forEach(builder::addClassPaths);

    configuration.getExecutedTests()
        .forEach(builder::addExecutionTests);

    builder.setTestTimeLimit((int) configuration.getTestTimeLimitSeconds());

    return builder.build();
  }

  public static GrpcGene serialize(final Gene gene) {
    final GrpcGene.Builder builder = GrpcGene.newBuilder();

    gene.getBases()
        .stream()
        .map(Serializer::serialize)
        .forEach(builder::addBase);;

    return builder.build();
  }

  public static GrpcBase serialize(final Base base) {
    final GrpcBase.Builder builder = GrpcBase.newBuilder()
        .setOperation(serialize(base.getOperation()))
        .setLocation(serialize(base.getTargetLocation()));

    return builder.build();
  }

  public static GrpcOperation serialize(final Operation operation) {
    final GrpcOperation.Builder builder = GrpcOperation.newBuilder();
    if (operation instanceof DeleteOperation) {
      builder.setType(GrpcOperation.Type.DELETE);

    } else if (operation instanceof InsertOperation) {
      builder.setType(GrpcOperation.Type.INSERT);
      builder.setStatement(((InsertOperation) operation).getNode()
          .toString());

    } else if (operation instanceof ReplaceOperation) {
      builder.setType(GrpcOperation.Type.REPLACE);
      builder.setStatement(((ReplaceOperation) operation).getNode()
          .toString());

    } else {
      throw new IllegalArgumentException();
    }
    return builder.build();
  }

  public static GrpcASTLocation serialize(final ASTLocation location) {
    final JDTASTLocation jdtLocation = (JDTASTLocation) location;
    final ASTNode node = jdtLocation.getNode();

    final GrpcASTLocation.Builder builder = GrpcASTLocation.newBuilder();
    builder.addAllLocation(createTreePath(node));
    builder.setSourcePath(jdtLocation.getSourcePath().path.toString());

    return builder.build();
  }

  public static GrpcTestResults serialize(final TestResults results) {
    final GrpcTestResults.Builder builder = GrpcTestResults.newBuilder();

    if (results instanceof EmptyTestResults) {
      builder.setEmpty(true);
    } else {
      final Map<String, GrpcTestResult> values = results.getExecutedTestFQNs()
          .stream()
          .collect(
              Collectors.toMap(fpn -> fpn.value, fqn -> serialize(results.getTestResult(fqn))));

      builder.setEmpty(false)
          .putAllValue(values)
          .setBuildResults(serialize(results.getBuildResults()));
    }
    return builder.build();
  }

  public static GrpcTestResult serialize(final TestResult result) {
    final Map<String, GrpcCoverage> values = result.getExecutedTargetFQNs()
        .stream()
        .collect(Collectors.toMap(fpn -> fpn.value, fqn -> serialize(result.getCoverages(fqn))));

    final GrpcTestResult.Builder builder = GrpcTestResult.newBuilder()
        .setExecutedTestFQN(result.executedTestFQN.value)
        .setFailed(result.failed)
        .putAllCoverage(values);

    return builder.build();
  }

  public static GrpcCoverage serialize(final Coverage coverage) {
    final Stream<GrpcCoverage.Status> statuses = coverage.statuses.stream()
        .map(Serializer::serialize);

    final GrpcCoverage.Builder builder = GrpcCoverage.newBuilder()
        .setExecutedTargetFQN(coverage.executedTargetFQN.value)
        .addAllStatus(() -> statuses.iterator());

    return builder.build();
  }

  public static GrpcCoverage.Status serialize(final Coverage.Status status) {
    switch (status) {
      case COVERED:
        return GrpcCoverage.Status.COVERED;
      case EMPTY:
        return GrpcCoverage.Status.EMPTY;
      case NOT_COVERED:
        return GrpcCoverage.Status.NOT_COVERED;
      case PARTLY_COVERED:
        return GrpcCoverage.Status.PARTLY_COVERED;
      default:
        throw new IllegalArgumentException();
    }
  }

  public static GrpcBuildResults serialize(final BuildResults buildResults) {
    final Map<String, GrpcFullyQualifiedNames> map = buildResults.getBinaryStore()
        .getAll()
        .stream()
        .collect(Collectors.groupingBy(v -> v.getOriginPath().path.toString(),
            Collectors.collectingAndThen(Collectors.toList(), Serializer::serialize)));

    final GrpcBuildResults.Builder builder = GrpcBuildResults.newBuilder()
        .putAllSourcePathToFQN(map);

    return builder.build();
  }

  public static GrpcFullyQualifiedNames serialize(final Collection<JavaBinaryObject> objects) {
    final GrpcFullyQualifiedNames.Builder builder = GrpcFullyQualifiedNames.newBuilder();
    objects.stream()
        .map(JavaBinaryObject::getFqn)
        .map(fpn -> fpn.value)
        .forEach(builder::addName);
    return builder.build();
  }

  public static Gene deserialize(final GrpcGene gene) {
    final List<Base> bases = gene.getBaseList()
        .stream()
        .map(Serializer::deserialize)
        .collect(Collectors.toList());

    return new Gene(bases);
  }

  public static Base deserialize(final GrpcBase base) {
    return new Base(deserialize(base.getLocation()),
        deserialize(base.getOperation()));
  }

  public static ASTLocation deserialize(final GrpcASTLocation location) {
    final ProductSourcePath sourcePath = new ProductSourcePath(Paths.get(location.getSourcePath()));
    return new RemoteJDTASTLocation(sourcePath, location.getLocationList());
  }

  public static Operation deserialize(final GrpcOperation operation) {
    switch (operation.getType()) {
      case DELETE:
        return new DeleteOperation();
      case INSERT:
        return new InsertOperation(parseStatement(operation.getStatement()));
      case REPLACE:
        return new ReplaceOperation(parseStatement(operation.getStatement()));
      default:
        throw new RuntimeException("can not recognize operation type");
    }
  }

  public static Configuration deserialize(
      final GrpcConfiguration configuration) {
    final Path rootDir = Paths.get(configuration.getRootDir());
    final List<Path> productPaths = configuration.getProductPathsList()
        .stream()
        .map(Paths::get)
        .collect(Collectors.toList());
    final List<Path> testPaths = configuration.getTestPathsList()
        .stream()
        .map(Paths::get)
        .collect(Collectors.toList());
    final Configuration.Builder builder =
        new Configuration.Builder(rootDir, productPaths, testPaths);
    configuration.getClassPathsList()
        .stream()
        .map(Paths::get)
        .forEach(builder::addClassPath);
    configuration.getExecutionTestsList()
        .forEach(builder::addExecutionTest);
    builder.setTestTimeLimitSeconds(configuration.getTestTimeLimit());
    return builder.build();
  }

  public static TestResults deserialize(final GrpcTestResults results) {
    if (results.getEmpty()) {
      return EmptyTestResults.instance;
    }

    final TestResults testResults = new TestResults();
    results.getValueMap()
        .values()
        .stream()
        .map(Serializer::deserialize)
        .forEach(testResults::add);

    final Map<ProductSourcePath, Set<FullyQualifiedName>> map =
        deserialize(results.getBuildResults());
    testResults.setSourcePathToFQN(map::get);
    return testResults;
  }

  public static TestResult deserialize(final GrpcTestResult result) {
    final FullyQualifiedName fqn = new TestFullyQualifiedName(result.getExecutedTestFQN());
    final Map<FullyQualifiedName, Coverage> map = result.getCoverageMap()
        .values()
        .stream()
        .map(Serializer::deserialize)
        .collect(Collectors.toMap(c -> c.executedTargetFQN, c -> c));
    return new TestResult(fqn, result.getFailed(), map);
  }

  public static Coverage deserialize(final GrpcCoverage coverage) {
    final FullyQualifiedName fqn = new TargetFullyQualifiedName(coverage.getExecutedTargetFQN());
    final List<Coverage.Status> statuses = coverage.getStatusList()
        .stream()
        .map(Serializer::deserialize)
        .collect(Collectors.toList());
    return new Coverage(fqn, statuses);
  }

  public static Map<ProductSourcePath, Set<FullyQualifiedName>> deserialize(
      final GrpcBuildResults buildResults) {
    return buildResults.getSourcePathToFQNMap()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(e -> new ProductSourcePath(Paths.get(e.getKey())),
            e -> deserialize(e.getValue())));
  }

  public static Set<FullyQualifiedName> deserialize(final GrpcFullyQualifiedNames names) {
    return names.getNameList()
        .stream()
        .map(TargetFullyQualifiedName::new)
        .collect(Collectors.toSet());
  }

  public static Coverage.Status deserialize(final GrpcCoverage.Status status) {
    switch (status) {
      case COVERED:
        return Coverage.Status.COVERED;
      case EMPTY:
        return Coverage.Status.EMPTY;
      case NOT_COVERED:
        return Coverage.Status.NOT_COVERED;
      case PARTLY_COVERED:
        return Coverage.Status.PARTLY_COVERED;
      default:
        throw new IllegalArgumentException();
    }
  }

  private static List<GrpcTreePathElement> createTreePath(final ASTNode current) {
    final ASTNode parent = current.getParent();
    if (parent == null) {
      return new ArrayList<>();
    }

    final List<GrpcTreePathElement> path = createTreePath(parent);
    final StructuralPropertyDescriptor locationInParent = current.getLocationInParent();
    final GrpcTreePathElement.Builder builder = GrpcTreePathElement.newBuilder();
    builder.setPropertyId(locationInParent.getId());
    if (locationInParent.isChildListProperty()) {
      final List<?> children = (List<?>) parent.getStructuralProperty(locationInParent);
      final int idx = children.indexOf(current);
      builder.setIndex(idx);
    }
    path.add(builder.build());
    return path;
  }

  private static Statement parseStatement(final String statement) {
    final ASTParser parser = JDTASTConstruction.createNewParser();
    parser.setKind(ASTParser.K_STATEMENTS);
    parser.setSource(statement.toCharArray());
    final ASTNode block = parser.createAST(null);
    final Object result = ((Block) block).statements()
        .get(0);
    return (Statement) result;
  }
}
