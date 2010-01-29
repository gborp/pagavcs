package hu.pagavcs.bl;

/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
public class Pair<A, B> {

	private A first;
	private B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	public A getFirst() {
		return first;
	}

	public void setFirst(A first) {
		this.first = first;
	}

	public B getSecond() {
		return second;
	}

	public void setSecond(B second) {
		this.second = second;
	}

	public String toString() {
		return "(" + first + ", " + second + ")";
	}

	private static boolean equals(Object x, Object y) {
		return (x == null && y == null) || (x != null && x.equals(y));
	}

	@SuppressWarnings("unchecked")
	public boolean equals(Object other) {
		return other instanceof Pair && equals(first, ((Pair) other).first) && equals(second, ((Pair) other).second);
	}

	public int hashCode() {
		if (first == null)
			return (second == null) ? 0 : second.hashCode() + 1;
		else if (second == null)
			return first.hashCode() + 2;
		else
			return first.hashCode() * 17 + second.hashCode();
	}

}
