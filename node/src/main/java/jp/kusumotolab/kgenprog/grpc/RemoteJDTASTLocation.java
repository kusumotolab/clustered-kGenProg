package jp.kusumotolab.kgenprog.grpc;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import jp.kusumotolab.kgenprog.project.GeneratedAST;
import jp.kusumotolab.kgenprog.project.LineNumberRange;
import jp.kusumotolab.kgenprog.project.SourcePath;
import jp.kusumotolab.kgenprog.project.jdt.JDTASTLocation;

public class RemoteJDTASTLocation extends JDTASTLocation {

  private final List<GrpcTreePathElement> pathToLocation;

  public RemoteJDTASTLocation(final SourcePath sourcePath,
      final List<GrpcTreePathElement> pathToLocation) {
    super(sourcePath, null, null);
    this.pathToLocation = pathToLocation;
  }

  @Override
  public ASTNode locate(final ASTNode otherASTRoot) {
    ASTNode current = otherASTRoot;
    for (final GrpcTreePathElement element : pathToLocation) {
      current = moveToChild(current, element);
    }
    return current;
  }

  @Override
  public LineNumberRange inferLineNumbers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public GeneratedAST<?> getGeneratedAST() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ASTNode getNode() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  private ASTNode moveToChild(final ASTNode current, final GrpcTreePathElement element) {
    final List<StructuralPropertyDescriptor> properties = current.structuralPropertiesForType();

    final StructuralPropertyDescriptor descriptor = properties.stream()
        .filter(v -> v.getId()
            .equals(element.getPropertyId()))
        .findFirst()
        .orElseThrow(RuntimeException::new);

    if (descriptor.isChildListProperty()) {
      final List<ASTNode> children = (List<ASTNode>) current.getStructuralProperty(descriptor);
      return children.get(element.getIndex());
    } else {
      return (ASTNode) current.getStructuralProperty(descriptor);
    }
  }

  public List<GrpcTreePathElement> getPathToLocation() {
    return pathToLocation;
  }
}
