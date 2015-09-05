

def solveX(string):
	left, right = string.split("=")
	print left, right


if __name__ == "__main__":
	test = " x + 9 – 2 -4 + x = – x + 5 – 1 + 3 – x "
	solveX(test)

