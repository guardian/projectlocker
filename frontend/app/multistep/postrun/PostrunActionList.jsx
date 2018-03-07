import React from 'react';
import PropTypes from 'prop-types';

class PostrunActionList extends React.Component {
    static propTypes = {
        actionList: PropTypes.array.isRequired,
        selectedActions: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);
    }

    render(){
        return <ul className="postrun-action-list">
            {this.props.actionList
                .filter(entry=>this.props.selectedActions.includes(entry.id))
                .map(entry=><li className="postrun-action-list">{entry.title}</li>)
            }
        </ul>
    }
}

export default PostrunActionList;