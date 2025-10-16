#!/bin/bash
# 脚本功能：使用 cypher-shell 导入 neo4j-import.cypher 文件中的数据到 neo4j 数据库

CYPHER_SHELL="$HOME/Library/Application Support/neo4j-desktop/Application/Data/dbmss/dbms-e097ec7c-8da5-4775-ac55-f1a4accaca94/bin/cypher-shell"
CYPHER_FILE="extract_out/neo4j-import.cypher"
NEO4J_PASSWORD="test654321"

echo "==================== Neo4j 批量导入工具 ===================="
echo "使用 cypher-shell 导入大文件"
echo ""
echo "开始导入，这可能需要几分钟..."
echo ""

"$CYPHER_SHELL" -u neo4j -p "$NEO4J_PASSWORD" -f "$CYPHER_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 导入成功！"
    echo "你现在可以在 Neo4j Browser 中查询数据了"
else
    echo ""
    echo "❌ 导入失败，请检查："
    echo "1. 数据库是否正在运行？"
    echo "2. 密码是否正确？"
    echo "3. 查看上面的错误信息"
fi
