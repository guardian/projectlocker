import React from 'react';
import PropTypes from 'prop-types';
import StorageEntryView from '../../EntryViews/StorageEntryView.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        projectTemplates: PropTypes.array.isRequired,
        selectedProjectTemplate: PropTypes.number.isRequired,
        storages: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        projectName: PropTypes.string.isRequired,
        projectFilename: PropTypes.string.isRequired,
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>New project name</td>
                <td id="project-name">{this.props.projectName}</td>
            </tr>
            <tr>
                <td>New file name</td>
                <td id="project-name">{this.props.projectFilename}</td>
            </tr>
            <tr>
                <td>Project template</td>
                {/*FIXME: replace this wil an EntryView*/}
                <td id="project-template-id">{this.props.selectedProjectTemplate}</td>
            </tr>
            <tr>
                <td>Storage</td>
                <td id="storage"><StorageEntryView entryId={this.props.selectedStorage}/></td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;