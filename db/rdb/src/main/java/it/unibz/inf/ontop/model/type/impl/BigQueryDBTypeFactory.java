package it.unibz.inf.ontop.model.type.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import it.unibz.inf.ontop.model.type.*;
import it.unibz.inf.ontop.model.vocabulary.XSD;

import java.util.Map;

import static it.unibz.inf.ontop.model.type.DBTermType.Category.*;
import static it.unibz.inf.ontop.model.type.impl.NonStringNonNumberNonBooleanNonDatetimeDBTermType.StrictEqSupport.NOTHING;
import static it.unibz.inf.ontop.model.type.impl.NonStringNonNumberNonBooleanNonDatetimeDBTermType.StrictEqSupport.SAME_TYPE_NO_CONSTANT;

public class BigQueryDBTypeFactory extends DefaultSQLDBTypeFactory {
    private static final String INT64_STR = "INT64";
    private static final String FLOAT64_STR = "FLOAT64";
    private static final String STRING_STR = "STRING";

    protected static final String GEOGRAPHY_STR = "GEOGRAPHY";
    protected static final String JSON_STR = "JSON";
    protected static final String BYTES_STR = "BYTES";

    protected BigQueryDBTypeFactory(Map<String, DBTermType> typeMap, ImmutableMap<DefaultTypeCode, String> defaultTypeCodeMap) {
        super(typeMap, defaultTypeCodeMap);
    }
    @AssistedInject
    protected BigQueryDBTypeFactory(@Assisted TermType rootTermType, @Assisted TypeFactory typeFactory) {
        super(createBigQueryTypeMap(rootTermType, typeFactory), createBigQueryCodeMap());
    }

    protected static Map<String, DBTermType> createBigQueryTypeMap(TermType rootTermType, TypeFactory typeFactory) {
        TermTypeAncestry rootAncestry = rootTermType.getAncestry();
        RDFDatatype xsdInteger = typeFactory.getXsdIntegerDatatype();
        RDFDatatype xsdDouble = typeFactory.getXsdDoubleDatatype();
        RDFDatatype xsdString = typeFactory.getXsdStringDatatype();
        RDFDatatype xsdBoolean = typeFactory.getXsdBooleanDatatype();
        RDFDatatype hexBinary = typeFactory.getDatatype(XSD.HEXBINARY);

        Map<String, DBTermType> map = createDefaultSQLTypeMap(rootTermType, typeFactory);

        DBTermType dateType = new DateDBTermType(DATE_STR, rootAncestry,
                typeFactory.getDatatype(XSD.DATE));

        NumberDBTermType int64Type = new NumberDBTermType(INT64_STR, rootAncestry,
                xsdInteger, INTEGER);

        NumberDBTermType float64Type = new NumberDBTermType(FLOAT64_STR, rootAncestry,
                xsdDouble, FLOAT_DOUBLE);

        StringDBTermType stringType = new StringDBTermType(STRING_STR, rootAncestry, xsdString);

        NonStringNonNumberNonBooleanNonDatetimeDBTermType bytesType = new NonStringNonNumberNonBooleanNonDatetimeDBTermType(BYTES_STR, rootAncestry, hexBinary);
                
        map.put(DATE_STR, dateType);
        map.put(INT64_STR, int64Type);
        map.put(FLOAT64_STR, float64Type);
        map.put(STRING_STR, stringType);
        map.put(BYTES_STR, bytesType);

        map.put(GEOGRAPHY_STR, new NonStringNonNumberNonBooleanNonDatetimeDBTermType(GEOGRAPHY_STR, rootAncestry, xsdString));
        map.put(JSON_STR, new JsonDBTermTypeImpl(JSON_STR, rootAncestry));

        map.remove(DOUBLE_PREC_STR);
        map.remove(VARCHAR_STR);
        map.remove(NVARCHAR_STR);
        map.remove(BINARY_LARGE_STR);
        map.remove(BINARY_STR);
        map.remove(BINARY_VAR_STR);

        return map;
    }

    protected static ImmutableMap<DefaultTypeCode, String> createBigQueryCodeMap() {
        Map<DefaultTypeCode, String> map = createDefaultSQLCodeMap();
        map.put(DefaultTypeCode.DOUBLE, FLOAT64_STR);
        map.put(DefaultTypeCode.STRING, STRING_STR);
        map.put(DefaultTypeCode.GEOGRAPHY, GEOGRAPHY_STR);
        map.put(DefaultTypeCode.JSON, JSON_STR);
        map.put(DefaultTypeCode.HEXBINARY, BYTES_STR);

        return ImmutableMap.copyOf(map);
    }




    @Override
    public boolean supportsDBGeometryType() {
        return false;
    }

    @Override
    public boolean supportsDBGeographyType() {
        return true;
    }

    @Override
    public boolean supportsDBDistanceSphere() {
        return false;
    }

    @Override
    public boolean supportsJson() {
        return true;
    }

    @Override
    public boolean supportsArrayType() {
        return true;
    }

}
