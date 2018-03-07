import React from 'react';
import PropTypes from 'prop-types';

class SummaryComponent extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        description: PropTypes.string.isRequired
    };

    render() {
        return <table>
            <tbody>
            <tr>
                <td>Postrun action title</td>
                <td>{this.props.title}</td>
            </tr>
            <tr>
                <td>Description</td>
                <td>{this.props.description}</td>
            </tr>
            </tbody>
        </table>
    }
}

export default SummaryComponent;