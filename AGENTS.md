# Bookstore Inventory Management System

## Overview:

You are in the role of a contractor who designs and develops the first version of the Bookstore
Inventory Management System for a customer. The Bookstore Inventory Management System
is a web application (REST API only) that allows bookstore owners to manage their inventory
efficiently. It provides features for adding, updating, and searching for books. The system will
be built using any JAVA technology or framework of your preference.

Approach the project as a real task that you are doing for a real customer. Think about the
future of the project and the consequences of your decisions.


##  Key Features:
1. Book CRUD Operations [REST API]:
	o Add new books with details like title, author, genre, and price.
	o Update existing book information.
	o Delete books from the inventory.
2. Search Functionality [REST API]:
	o Search for books by title, author, or genre.
	o Display search results in a paginated format.
3. Authentication and Authorization:
	o Implement basic authentication for bookstore staff.
	o Differentiate between admin and regular users.
	o Admins can perform all CRUD operations, while regular users can only view books.
4. Database:
	o Set up a database to store book information.
	o Define appropriate tables and relationships (e.g., Book, Author, Genre).

## More resources:
- API contract: libs/api-contract/src/main/resources/openapi/catalog-service-api.yaml
- Developer-facing summary: API_GUIDE.md