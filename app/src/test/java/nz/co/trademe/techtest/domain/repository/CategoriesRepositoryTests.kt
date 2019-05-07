package nz.co.trademe.techtest.domain.repository

import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.wrapper.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations


class CategoriesRepositoryTests {

	@Mock
	lateinit var networkRepository: NetworkRepository
	lateinit var repository: CategoriesRepository

	@Before
	fun init() {
		MockitoAnnotations.initMocks(this)
		repository = CategoriesRepository(networkRepository)
	}

	@Test
	fun getSubcategory_firstLevelCategory_returnsCategory() {
		// verify that when searching for a category on the first level under the root, that the category is correctly
		// returned

		val category1111 = Category("1111-", "", false, null)
		val category1112 = Category("1112-", "", false, null)

		val categoryRoot = Category("", "", false, listOf(category1111, category1112))

		assertEquals(category1112, repository.getSubcategory("1112-", categoryRoot))
	}

	@Test
	fun getSubcategory_secondLevelCategory_returnsCategory() {
		// verify that when searching for a category on the second level under the root, that the category is correctly
		// returned

		val category1111 = Category("1111-", "", false, null)

		val category2221 = Category("1112-2221-", "", false, null)
		val category2222 = Category("1112-2222-", "", false, null)
		val category1112 = Category("1112-", "", false, listOf(category2221, category2222))

		val categoryRoot = Category("", "", false, listOf(category1111, category1112))

		// parent doesn't match target category number path
		assertEquals(category2222, repository.getSubcategory("1112-2222-", categoryRoot))
	}

	@Test
	fun getNextSubcategoryNumber_validInput_returnsExpectedValues() {
		// initial case of searching from root
		assertEquals("1111-", repository.getNextSubcategoryNumber("1111-2222-", ""))

		// common case of searching down category tree
		assertEquals("1111-2222-", repository.getNextSubcategoryNumber("1111-2222-", "1111-"))

		// termination case of having no further categories to search
		assertNull(repository.getNextSubcategoryNumber("1111-2222-", "1111-2222-"))
	}

	@Test
	fun getNextSubcategoryNumber_invalidInput_returnsNull() {
		// parent doesn't match target category number path
		assertNull(repository.getNextSubcategoryNumber("1111-", "9999-8888-"))

		// no target category provided
		assertNull(repository.getNextSubcategoryNumber("", "1111-2222-"))
	}
}