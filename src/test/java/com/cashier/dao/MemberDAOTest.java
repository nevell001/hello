package com.cashier.dao;

import com.cashier.model.Member;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MemberDAOTest extends DatabaseTestBase {

    private final MemberDAORefactored memberDAO = DAOFactory.getInstance().getMemberDAO();

    @BeforeEach
    void setUp() throws Exception {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        clearTestData();
    }

    @Test
    @DisplayName("会员搜索应返回会员编号")
    void searchShouldMapMemberCode() throws Exception {
        Member member = createMember("MEM202606170001", "13800138001", "搜索会员", "银卡");

        List<Member> result = memberDAO.search("搜索会员");

        assertFalse(result.isEmpty());
        assertEquals(member.memberCode, result.get(0).memberCode);
    }

    @Test
    @DisplayName("按等级查询会员应返回会员编号")
    void findByLevelShouldMapMemberCode() throws Exception {
        Member member = createMember("MEM202606170002", "13800138002", "等级会员", "金卡");

        List<Member> result = memberDAO.findByLevel("金卡");

        assertFalse(result.isEmpty());
        assertEquals(member.memberCode, result.get(0).memberCode);
    }

    private Member createMember(String memberCode, String phone, String name, String level) throws Exception {
        Member member = new Member();
        member.memberCode = memberCode;
        member.phone = phone;
        member.name = name;
        member.points = BigDecimal.valueOf(1000);
        member.level = level;
        member.discount = BigDecimal.valueOf(9.5);
        member.balance = BigDecimal.valueOf(100);
        member.birthday = "";

        memberDAO.insert(member);
        return memberDAO.findByPhone(phone);
    }
}
