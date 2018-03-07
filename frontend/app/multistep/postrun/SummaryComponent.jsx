import React from 'react';
import PropTypes from 'prop-types';
import PostrunActionList from "./PostrunActionList.jsx";

class SummaryComponent extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        description: PropTypes.string.isRequired,
        actionList: PropTypes.array.isRequired,
        selectedActions: PropTypes.array.isRequired
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
            <tr>
                <td>Dependencies</td>
                <td><PostrunActionList actionList={this.props.actionList}
                                       selectedActions={this.props.selectedActions}/>
                </td>
            </tr>
            </tbody>
        </table>
    }
}

export default SummaryComponent;