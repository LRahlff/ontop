package it.unibz.inf.ontop.dbschema;

import com.google.common.collect.ImmutableList;

import java.sql.SQLException;

public interface RDBMetadataLoader {
    ImmutableList<RelationID> getRelationIDs() throws SQLException;

    ImmutableList<RelationID> getRelationIDs(ImmutableList<RelationID> seed);

    /**
     * relationID can be mapped to many tables (if, for example, it has no schema)
     *
     * @param relationID
     * @return
     */
    ImmutableList<RelationDefinition.AttributeListBuilder> getRelationAttributes(RelationID relationID) throws SQLException;

    void insertIntegrityConstraints(RelationDefinition relation);
}
