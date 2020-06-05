import React from 'react';
import PropTypes from 'prop-types';
import WorkingGroupEntryView from '../../EntryViews/WorkingGroupEntryView.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        commissionName: PropTypes.string.isRequired,
        wgList: PropTypes.array.isRequired,
        selectedWorkingGroupId: PropTypes.number.isRequired,
        createTime: PropTypes.string.isRequired,
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>New commission name</td>
                <td id="project-name">{this.props.commissionName}</td>
            </tr>
            <tr>
                <td>Working group</td>
                <td id="working-group"><WorkingGroupEntryView entryId={this.props.selectedWorkingGroupId}/></td>
            </tr>
            <tr>
                <td>Creation time</td>
                <td id="create-time">{this.props.createTime}</td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;