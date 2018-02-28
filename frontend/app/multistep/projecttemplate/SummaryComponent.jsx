import React from 'react';
import PropTypes from 'prop-types';
import ProjectTypeView from '../../EntryViews/ProjectTypeView.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        fileId: PropTypes.number.isRequired,
        projectType: PropTypes.number.isRequired,
        name: PropTypes.string.isRequired
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>Template name</td>
                <td id="template-name">{this.props.name}</td>
            </tr>
            <tr>
                <td>File</td>
                <td id="fileId">{this.props.fileId}</td>
            </tr>
            <tr>
                <td>Project type</td>
                <td id="projectType"><ProjectTypeView entryId={this.props.projectType}/></td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;